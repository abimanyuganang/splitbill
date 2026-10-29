
package com.example.splitbill.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

public final class BillDtos {

    private BillDtos() {}

    public record CreateGroupRequest(
        @NotBlank String name,
        @NotEmpty List<@NotBlank String> participants
    ) {}

    public record AddExpenseRequest(
        @NotBlank String description,
        @NotBlank String paidBy,
        @NotNull @DecimalMin("1") BigDecimal amount,
        @NotEmpty List<@NotBlank String> splitAmong
    ) {}

    public record GroupResponse(
        UUID id,
        String name,
        List<String> participants,
        int expenseCount
    ) {}

    public record ExpenseResponse(
        UUID id,
        String description,
        String paidBy,
        BigDecimal amount,
        List<String> splitAmong
    ) {}

    public record BalanceResponse(
        String participant,
        BigDecimal paid,
        BigDecimal share,
        BigDecimal balance
    ) {}

    public record TransferResponse(
        String from,
        String to,
        BigDecimal amount
    ) {}

    public record SettlementResponse(
        UUID groupId,
        BigDecimal totalExpenses,

        @JsonProperty("service_charge_pct")
        int serviceChargePct,

        @JsonProperty("service_charge_amount")
        BigDecimal serviceChargeAmount,

        List<BalanceResponse> balances,
        List<TransferResponse> settlements
    ) {}
}
