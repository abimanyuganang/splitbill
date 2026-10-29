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
        // Alice pays for everyone.
        billService.addExpense(
            groupId,
            new AddExpenseRequest(
                "Dinner",
                "Alice",
                new BigDecimal("90000"),
                List.of("Alice", "Bob", "Charlie")
            )
        );

        // Bob pays 60.000
        billService.addExpense(
            groupId,
            new AddExpenseRequest(
                "Transport",
                "Bob",
                new BigDecimal("60000"),
                List.of("Alice", "Bob", "Charlie")
            )
        );

        SettlementResponse result =
            billService.getSettlement(groupId);

        assertEquals(
            new BigDecimal("150000"),
            result.totalExpenses()
        );

        assertEquals(4, result.serviceChargePct());

        assertEquals(
            new BigDecimal("6000"),
            result.serviceChargeAmount()
        );

        assertEquals(2, result.settlements().size());

        assertEquals(
            new TransferResponse(
                "Charlie",
                "Alice",
                new BigDecimal("40000")
            ),
            result.settlements().get(0)
        );

        assertEquals(
            new TransferResponse(
                "Charlie",
                "Bob",
                new BigDecimal("10000")
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
                new BigDecimal("100000"),
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
            new BigDecimal("100000"),
            totalShares
        );

        assertEquals(
            new BigDecimal("33334"),
            result.balances().get(0).share()
        );
    }
}
