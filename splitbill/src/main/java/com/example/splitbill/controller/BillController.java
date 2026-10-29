
package com.example.splitbill.controller;

import com.example.splitbill.dto.BillDtos.*;
import com.example.splitbill.service.BillService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse createGroup(
        @Valid @RequestBody CreateGroupRequest request
    ) {
        return billService.createGroup(request);
    }

    @GetMapping("/{groupId}")
    public GroupResponse getGroup(
        @PathVariable UUID groupId
    ) {
        return billService.getGroup(groupId);
    }

    @PostMapping("/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse addExpense(
        @PathVariable UUID groupId,
        @Valid @RequestBody AddExpenseRequest request
    ) {
        return billService.addExpense(groupId, request);
    }

    @GetMapping("/{groupId}/settlement")
    public SettlementResponse getSettlement(
        @PathVariable UUID groupId
    ) {
        return billService.getSettlement(groupId);
    }
}
