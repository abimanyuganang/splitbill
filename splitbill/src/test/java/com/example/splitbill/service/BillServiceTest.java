package com.example.splitbill.service;

import com.example.splitbill.dto.BillDtos.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BillServiceTest {

    private BillService billService;
    private UUID groupId;

    @BeforeEach
    void setUp() {
        // "abc" = 97 + 98 + 99 = 294
        // 294 % 10 = 4%
        billService = new BillService("abc");

        GroupResponse group = billService.createGroup(
            new CreateGroupRequest(
                "Trip",
                List.of("Alice", "Bob", "Charlie")
            )
        );

        groupId = group.id();
    }

    @Test
    void shouldCalculateCorrectSettlement() {
        // Alice pays RM90 for everyone.
        billService.addExpense(
            groupId,
            new AddExpenseRequest(
                "Dinner",
                "Alice",
                new BigDecimal("90.00"),
                List.of("Alice", "Bob", "Charlie")
            )
        );

        // Bob pays RM60 for everyone.
        billService.addExpense(
            groupId,
            new AddExpenseRequest(
                "Transport",
                "Bob",
                new BigDecimal("60.00"),
                List.of("Alice", "Bob", "Charlie")
            )
        );

        SettlementResponse result =
            billService.getSettlement(groupId);

        assertEquals(
            new BigDecimal("150.00"),
            result.totalExpenses()
        );

        assertEquals(4, result.serviceChargePct());

        assertEquals(
            new BigDecimal("6.00"),
            result.serviceChargeAmount()
        );

        assertEquals(2, result.settlements().size());

        assertEquals(
            new TransferResponse(
                "Charlie",
                "Alice",
                new BigDecimal("40.00")
            ),
            result.settlements().get(0)
        );

        assertEquals(
            new TransferResponse(
                "Charlie",
                "Bob",
                new BigDecimal("10.00")
            ),
            result.settlements().get(1)
        );
    }

    @Test
    void shouldDistributeRoundingRemainderCorrectly() {
        billService.addExpense(
            groupId,
            new AddExpenseRequest(
                "Lunch",
                "Alice",
                new BigDecimal("100.00"),
                List.of("Alice", "Bob", "Charlie")
            )
        );

        SettlementResponse result =
            billService.getSettlement(groupId);

        BigDecimal totalShares = result.balances()
            .stream()
            .map(BalanceResponse::share)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(
            new BigDecimal("100.00"),
            totalShares
        );

        assertEquals(
            new BigDecimal("33.34"),
            result.balances().get(0).share()
        );
    }
}
