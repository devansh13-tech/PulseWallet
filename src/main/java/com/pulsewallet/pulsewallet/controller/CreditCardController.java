package com.pulsewallet.pulsewallet.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.BankConsentRequest;
import com.pulsewallet.pulsewallet.dto.BankConsentResponse;
import com.pulsewallet.pulsewallet.dto.BankSyncStatusResponse;
import com.pulsewallet.pulsewallet.dto.CardBenefitsResponse;
import com.pulsewallet.pulsewallet.dto.CardIdentificationRequest;
import com.pulsewallet.pulsewallet.dto.CardIdentificationResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardIntelligenceResponse;
import com.pulsewallet.pulsewallet.dto.CreditCardRequest;
import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckRequest;
import com.pulsewallet.pulsewallet.dto.PurchaseCheckResponse;
import com.pulsewallet.pulsewallet.dto.StatementConfirmationRequest;
import com.pulsewallet.pulsewallet.dto.StatementExtractionResponse;
import com.pulsewallet.pulsewallet.security.UserPrincipal;
import com.pulsewallet.pulsewallet.service.CreditCardIntelligenceService;
import com.pulsewallet.pulsewallet.service.CreditCardService;
import com.pulsewallet.pulsewallet.service.bankdata.BankDataIntegrationProvider;
import com.pulsewallet.pulsewallet.service.cardid.CardIdentificationService;
import com.pulsewallet.pulsewallet.service.statement.StatementPdfParserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardIntelligenceService intelligenceService;
    private final CardIdentificationService cardIdentificationService;
    private final StatementPdfParserService statementPdfParserService;
    private final BankDataIntegrationProvider bankDataIntegrationProvider;

    public CreditCardController(
            CreditCardService creditCardService,
            CreditCardIntelligenceService intelligenceService,
            CardIdentificationService cardIdentificationService,
            StatementPdfParserService statementPdfParserService,
            BankDataIntegrationProvider bankDataIntegrationProvider) {
        this.creditCardService = creditCardService;
        this.intelligenceService = intelligenceService;
        this.cardIdentificationService = cardIdentificationService;
        this.statementPdfParserService = statementPdfParserService;
        this.bankDataIntegrationProvider = bankDataIntegrationProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreditCardResponse> create(@Valid @RequestBody CreditCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Credit card created", creditCardService.create(principal.getId(), request));
    }

    @GetMapping
    public ApiResponse<List<CreditCardResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(creditCardService.getAll(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CreditCardResponse> get(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(creditCardService.getById(id, principal.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<CreditCardResponse> update(@PathVariable Long id,
            @Valid @RequestBody CreditCardRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Credit card updated", creditCardService.update(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        creditCardService.delete(id, principal.getId());
        return ApiResponse.message("Credit card deleted");
    }

    @GetMapping("/intelligence")
    public ApiResponse<CreditCardIntelligenceResponse> intelligence(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(intelligenceService.getIntelligence(principal.getId()));
    }

    @PostMapping("/purchase-check")
    public ApiResponse<PurchaseCheckResponse> purchaseCheck(@Valid @RequestBody PurchaseCheckRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(intelligenceService.checkPurchase(principal.getId(), request));
    }

    @PostMapping("/identify")
    public ApiResponse<CardIdentificationResponse> identify(
            @Valid @RequestBody CardIdentificationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Card identified", cardIdentificationService.identify(request));
    }

    @PostMapping(value = "/statements/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StatementExtractionResponse> parseStatement(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Statement parsed successfully", statementPdfParserService.parseStatementPdf(file));
    }

    @PostMapping("/statements/confirm")
    public ApiResponse<CreditCardResponse> confirmStatement(
            @Valid @RequestBody StatementConfirmationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Statement confirmed and saved", statementPdfParserService.confirmAndSave(principal.getId(), request));
    }

    @GetMapping("/{id}/benefits")
    public ApiResponse<CardBenefitsResponse> getBenefits(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(intelligenceService.getCardBenefits(id, principal.getId()));
    }

    @GetMapping("/bank-sync/status")
    public ApiResponse<BankSyncStatusResponse> getBankSyncStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(bankDataIntegrationProvider.getIntegrationStatus());
    }

    @PostMapping("/bank-sync/consent")
    public ApiResponse<BankConsentResponse> initiateBankConsent(
            @Valid @RequestBody BankConsentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(bankDataIntegrationProvider.initiateConsent(principal.getId(), request));
    }
}
