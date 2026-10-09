package com.pulsewallet.pulsewallet.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.BankIngestionDtos.*;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.BankIngestionService;

@RestController
@RequestMapping("/api/bank-ingestion")
public class BankIngestionController {

    private final BankIngestionService bankIngestionService;

    public BankIngestionController(BankIngestionService bankIngestionService) {
        this.bankIngestionService = bankIngestionService;
    }

    @PostMapping("/parse-sms")
    public ApiResponse<ParsedTransactionDto> parseSms(@RequestBody SmsParseRequest request) {
        return ApiResponse.ok("SMS parsed successfully", bankIngestionService.parseSms(request.smsText()));
    }

    @PostMapping("/account-aggregator/consent")
    public ApiResponse<AccountAggregatorConsentResponse> initiateAaConsent(
            @RequestBody AccountAggregatorConsentRequest request) {
        return ApiResponse.ok("Account Aggregator consent active", bankIngestionService.initiateAccountAggregator(request));
    }

    @PostMapping("/category-feedback")
    public ApiResponse<String> submitCategoryFeedback(@RequestBody CategoryFeedbackRequest request) {
        bankIngestionService.recordCategoryFeedback(request.merchant(), request.correctedCategory());
        return ApiResponse.ok("Categorizer updated with preference");
    }

    @PostMapping("/import-transactions")
    public ApiResponse<Integer> batchImport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody BatchImportRequest request) {
        List<Transaction> imported = bankIngestionService.batchImport(principal.getId(), request.transactions());
        return ApiResponse.ok("Imported " + imported.size() + " transactions successfully", imported.size());
    }
}
