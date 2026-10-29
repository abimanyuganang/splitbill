
package com.example.splitbill.service;

import com.example.splitbill.dto.BillDtos.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class BillService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final Map<UUID, BillGroup> groups = new HashMap<>();
    private final int serviceChargePct;

    public BillService( @Value("${app.github-username}") String githubUsername) {
        if (githubUsername == null || githubUsername.isBlank()) {
            throw new IllegalArgumentException(
                "GitHub username must be configured"
            );
        }

        // Compute the percentage dynamically from the username.
        int sum = githubUsername.toLowerCase(Locale.ROOT)
            .codePoints()
            .sum();

        this.serviceChargePct = sum % 10;
    }

    // Internal data structures
    private record Expense(
        UUID id,
        String description,
        String paidBy,
        BigDecimal amount,
        List<String> splitAmong
    ) {}

    private static class BillGroup {
        UUID id;
        String name;
        List<String> participants;
        List<Expense> expenses = new ArrayList<>();

        BillGroup(UUID id, String name, List<String> participants) {
            this.id = id;
            this.name = name;
            this.participants = participants;
        }
    }

    // 1. Create a group 
    public synchronized GroupResponse createGroup(
        CreateGroupRequest request
    ) {
        List<String> participants = request.participants()
            .stream()
            .map(String::trim)
            .toList();

        if (new HashSet<>(participants).size()
                != participants.size()) {
            throw new IllegalArgumentException(
                "Participant names must be unique"
            );
        }

        UUID id = UUID.randomUUID();

        BillGroup group = new BillGroup(
            id,
            request.name().trim(),
            participants
        );

        groups.put(id, group);

        return toGroupResponse(group);
    }

    // 2. Retrieve a group
    public synchronized GroupResponse getGroup(UUID groupId) {
        return toGroupResponse(findGroup(groupId));
    }

    // 3. Add an expense
    public synchronized ExpenseResponse addExpense(UUID groupId, AddExpenseRequest request) {
        BillGroup group = findGroup(groupId);

        String payer = request.paidBy().trim();

        if (!group.participants.contains(payer)) {
            throw new IllegalArgumentException(
                "Payer must belong to the group"
            );
        }

        List<String> beneficiaries = request.splitAmong()
            .stream()
            .map(String::trim)
            .toList();

        if (new HashSet<>(beneficiaries).size()
                != beneficiaries.size()) {
            throw new IllegalArgumentException(
                "Split participants must be unique"
            );
        }

        for (String person : beneficiaries) {
            if (!group.participants.contains(person)) {
                throw new IllegalArgumentException(
                    "Unknown participant: " + person
                );
            }
        }

        // Reject values with more than two decimal places
        // instead of silently rounding users' money.
        BigDecimal amount = request.amount()
            .setScale(0, RoundingMode.UNNECESSARY);

        Expense expense = new Expense(
            UUID.randomUUID(),
            request.description().trim(),
            payer,
            amount,
            List.copyOf(beneficiaries)
        );

        group.expenses.add(expense);

        return new ExpenseResponse(
            expense.id(),
            expense.description(),
            expense.paidBy(),
            expense.amount(),
            expense.splitAmong()
        );
    }

    // 4. Calculate the settlement
    public synchronized SettlementResponse getSettlement(UUID groupId) {
        BillGroup group = findGroup(groupId);

        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        Map<String, BigDecimal> shares = new LinkedHashMap<>();

        for (String participant : group.participants) {
            paid.put(participant, ZERO);
            shares.put(participant, ZERO);
        }

        BigDecimal totalExpenses = ZERO;

        // Calculate money paid and each person's fair share.
        for (Expense expense : group.expenses) {
            totalExpenses = totalExpenses.add(expense.amount());

            paid.merge(
                expense.paidBy(),
                expense.amount(),
                BigDecimal::add
            );

            List<String> people = expense.splitAmong();

            // Split expense equally in whole rupiah
            BigDecimal[] result = expense.amount()
                .divideAndRemainder(
                    BigDecimal.valueOf(people.size())
                );

            BigDecimal amountPerPerson = result[0];
            int remainder = result[1].intValueExact();

            for (int i = 0; i < people.size(); i++) {
                BigDecimal share = amountPerPerson;

                // Distribute remaining rupiah fairly
                if (i < remainder) {
                    share = share.add(BigDecimal.ONE);
                }

                shares.merge(
                    people.get(i),
                    share,
                    BigDecimal::add
                );
            }
        }

        Map<String, BigDecimal> balances =
            new LinkedHashMap<>();

        List<BalanceResponse> balanceResponses =
            new ArrayList<>();

        for (String participant : group.participants) {
            BigDecimal balance = paid.get(participant)
                .subtract(shares.get(participant));

            balances.put(participant, balance);

            balanceResponses.add(new BalanceResponse(
                participant,
                paid.get(participant),
                shares.get(participant),
                balance
            ));
        }

        List<TransferResponse> settlements =
            calculateTransfers(balances);

        BigDecimal charge = totalExpenses
            .multiply(BigDecimal.valueOf(serviceChargePct))
            .divide(BigDecimal.valueOf(100), 0,RoundingMode.HALF_UP);

        return new SettlementResponse(
            group.id,
            totalExpenses,
            serviceChargePct,
            charge,
            balanceResponses,
            settlements
        );
    }

    // Greedy settlement algorithm
    private List<TransferResponse> calculateTransfers(
        Map<String, BigDecimal> balances
    ) {
        Map<String, BigDecimal> creditors =
            new LinkedHashMap<>();

        Map<String, BigDecimal> debtors =
            new LinkedHashMap<>();

        for (var entry : balances.entrySet()) {
            BigDecimal balance = entry.getValue();

            if (balance.signum() > 0) {
                creditors.put(entry.getKey(), balance);
            } else if (balance.signum() < 0) {
                debtors.put(entry.getKey(), balance.abs());
            }
        }

        List<TransferResponse> transfers = new ArrayList<>();

        for (String debtor : debtors.keySet()) {
            BigDecimal owed = debtors.get(debtor);

            for (String creditor : creditors.keySet()) {
                if (owed.signum() == 0) {
                    break;
                }

                BigDecimal receivable = creditors.get(creditor);

                if (receivable.signum() == 0) {
                    continue;
                }

                BigDecimal payment = owed.min(receivable);

                transfers.add(new TransferResponse(
                    debtor,
                    creditor,
                    payment
                ));

                owed = owed.subtract(payment);

                creditors.put(
                    creditor,
                    receivable.subtract(payment)
                );
            }
        }

        return transfers;
    }

    private BillGroup findGroup(UUID groupId) {
        BillGroup group = groups.get(groupId);

        if (group == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Group not found"
            );
        }

        return group;
    }

    private GroupResponse toGroupResponse(BillGroup group) {
        return new GroupResponse(
            group.id,
            group.name,
            group.participants,
            group.expenses.size()
        );
    }
}
