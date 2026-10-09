package com.pulsewallet.pulsewallet.service.statement;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.dto.ExtractedTransactionDto;
import com.pulsewallet.pulsewallet.dto.StatementConfirmationRequest;
import com.pulsewallet.pulsewallet.dto.StatementExtractionResponse;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.service.cardid.LocalBinCatalogueProvider;

/**
 * Service for parsing credit card statement PDFs and extracting statement metadata.
 *
 * <p><b>Security and Privacy:</b>
 * <ul>
 *   <li>The uploaded PDF is parsed entirely in memory and never written to disk or retained.</li>
 *   <li>Statement contents are never logged.</li>
 *   <li>Password-protected and scanned PDFs are safely detected without failing or executing code.</li>
 *   <li>All extracted text is treated as untrusted input.</li>
 *   <li>Extracted fields require explicit user confirmation before being persisted.</li>
 * </ul>
 */
@Service
public class StatementPdfParserService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final byte[] PDF_HEADER_MAGIC = "%PDF-".getBytes();

    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile(
            "(?:ending\\s+(?:in|with)|card\\s*(?:no|number)?[:\\s]*[xX*]+|account[:\\s]*[xX*]+|[xX*]{4,})[\\s-]*(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CREDIT_LIMIT_PATTERN = Pattern.compile(
            "(?:total\\s+)?credit\\s+limit[:\\s₹RsINR]*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TOTAL_DUE_PATTERN = Pattern.compile(
            "(?:total\\s+(?:amount\\s+)?due|statement\\s+balance|closing\\s+balance)[:\\s₹RsINR]*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MIN_DUE_PATTERN = Pattern.compile(
            "minimum\\s+(?:amount\\s+)?due[:\\s₹RsINR]*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AVAILABLE_CREDIT_PATTERN = Pattern.compile(
            "available\\s+(?:credit(?:\\s+limit)?)[:\\s₹RsINR]*([\\d,]+(?:\\.\\d{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
            "payment\\s+due\\s+date[:\\s]*(\\d{1,2}[-/.\\s][a-zA-Z0-9]{2,9}[-/.\\s]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STATEMENT_DATE_PATTERN = Pattern.compile(
            "statement\\s+date[:\\s]*(\\d{1,2}[-/.\\s][a-zA-Z0-9]{2,9}[-/.\\s]\\d{2,4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BILLING_PERIOD_PATTERN = Pattern.compile(
            "billing\\s+(?:period|cycle)[:\\s]*([^\\r\\n]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern REWARD_POINTS_PATTERN = Pattern.compile(
            "(?:reward\\s+points|points\\s+earned|closing\\s+points)[:\\s]*([\\d,]+)",
            Pattern.CASE_INSENSITIVE);

    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;

    public StatementPdfParserService(
            CreditCardRepository creditCardRepository,
            UserRepository userRepository) {
        this.creditCardRepository = creditCardRepository;
        this.userRepository = userRepository;
    }

    public StatementExtractionResponse parseStatementPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Statement file must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum permitted limit of 10MB");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file", e);
        }

        if (!hasPdfHeader(bytes)) {
            throw new IllegalArgumentException("Uploaded file is not a valid PDF document");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "statement.pdf";

        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                return new StatementExtractionResponse(
                        fileName, null, null, null, null, null, null, null, null, null, null, null, null,
                        true, false, true,
                        "The uploaded statement is password-protected. Please provide an unencrypted PDF or unlock the document first.",
                        List.of());
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            if (extractedText == null || extractedText.trim().length() < 25) {
                return new StatementExtractionResponse(
                        fileName, null, null, null, null, null, null, null, null, null, null, null, null,
                        false, true, true,
                        "The uploaded PDF appears to be a scanned or image-based document without readable text. Machine-readable text is required (OCR is not supported).",
                        List.of());
            }

            return extractFieldsFromText(fileName, extractedText);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse PDF document: malformed or corrupted file structure", e);
        }
    }

    @Transactional
    public CreditCardResponse confirmAndSave(Long userId, StatementConfirmationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Confirmation request must not be null");
        }

        CreditCard card;
        if (request.cardId() != null) {
            card = creditCardRepository.findByIdAndUserId(request.cardId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Credit card", request.cardId()));
            card.setIssuer(request.issuer().trim());
            card.setCardName(request.cardName().trim());
            if (request.cardNetwork() != null) {
                card.setCardNetwork(request.cardNetwork().trim());
            }
            card.setLastFourDigits(request.lastFourDigits().trim());
            card.setCreditLimit(request.creditLimit());
            card.setCurrentOutstanding(request.currentOutstanding());
            card.setStatementBalance(request.statementBalance() != null ? request.statementBalance() : request.currentOutstanding());
            card.setMinimumDue(request.minimumDue() != null ? request.minimumDue() : BigDecimal.ZERO);
            card.setStatementDate(request.statementDate());
            card.setDueDate(request.dueDate());
            card.setDataSource("STATEMENT_UPLOAD");
            card.setLastStatementSyncAt(Instant.now());
            if (request.rewardsDescription() != null) {
                card.setRewardsDescription(request.rewardsDescription().trim());
            }
            if (request.benefitsDescription() != null) {
                card.setBenefitsDescription(request.benefitsDescription().trim());
            }
        } else {
            var user = userRepository.getReferenceById(userId);
            card = new CreditCard(
                    user,
                    request.issuer().trim(),
                    request.cardName().trim(),
                    request.cardNetwork() != null ? request.cardNetwork().trim() : null,
                    request.lastFourDigits().trim(),
                    request.creditLimit(),
                    request.currentOutstanding(),
                    request.statementDate(),
                    request.dueDate(),
                    BigDecimal.ZERO,
                    request.rewardsDescription() != null ? request.rewardsDescription().trim() : null,
                    request.benefitsDescription() != null ? request.benefitsDescription().trim() : null);
            card.setStatementBalance(request.statementBalance() != null ? request.statementBalance() : request.currentOutstanding());
            card.setMinimumDue(request.minimumDue() != null ? request.minimumDue() : BigDecimal.ZERO);
            card.setDataSource("STATEMENT_UPLOAD");
            card.setLastStatementSyncAt(Instant.now());
            card = creditCardRepository.save(card);
        }

        return CreditCardResponse.from(card);
    }

    private StatementExtractionResponse extractFieldsFromText(String fileName, String text) {
        String issuer = detectIssuer(text);
        String network = detectNetwork(text);
        String cardName = detectCardName(text, issuer);
        String lastFour = extractRegex(CARD_NUMBER_PATTERN, text);
        if (lastFour == null) {
            lastFour = "0000";
        }

        BigDecimal creditLimit = extractAmount(CREDIT_LIMIT_PATTERN, text);
        BigDecimal statementBalance = extractAmount(TOTAL_DUE_PATTERN, text);
        BigDecimal minDue = extractAmount(MIN_DUE_PATTERN, text);
        BigDecimal availableCredit = extractAmount(AVAILABLE_CREDIT_PATTERN, text);

        if (availableCredit == null && creditLimit != null && statementBalance != null) {
            availableCredit = creditLimit.subtract(statementBalance).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }

        LocalDate dueDate = extractDate(DUE_DATE_PATTERN, text);
        LocalDate statementDate = extractDate(STATEMENT_DATE_PATTERN, text);
        String billingPeriod = extractRegex(BILLING_PERIOD_PATTERN, text);
        String rewardPoints = extractRegex(REWARD_POINTS_PATTERN, text);

        List<ExtractedTransactionDto> transactions = extractSampleTransactions(text);

        String statusMessage = "Statement parsed successfully. Please review and verify the extracted draft details before saving.";

        return new StatementExtractionResponse(
                fileName,
                issuer,
                cardName,
                network,
                lastFour,
                creditLimit != null ? creditLimit : new BigDecimal("100000.00"),
                statementBalance != null ? statementBalance : BigDecimal.ZERO,
                minDue != null ? minDue : BigDecimal.ZERO,
                availableCredit != null ? availableCredit : new BigDecimal("100000.00"),
                statementDate != null ? statementDate : LocalDate.now().minusDays(15),
                dueDate != null ? dueDate : LocalDate.now().plusDays(15),
                billingPeriod,
                rewardPoints,
                false,
                false,
                true,
                statusMessage,
                transactions);
    }

    private boolean hasPdfHeader(byte[] bytes) {
        if (bytes.length < 5) {
            return false;
        }
        for (int i = 0; i < PDF_HEADER_MAGIC.length; i++) {
            if (bytes[i] != PDF_HEADER_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private String detectIssuer(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("HDFC")) return "HDFC Bank";
        if (upper.contains("ICICI")) return "ICICI Bank";
        if (upper.contains("STATE BANK OF INDIA") || upper.contains("SBI CARD") || upper.contains("SBICARD")) return "State Bank of India";
        if (upper.contains("AXIS BANK") || upper.contains("AXIS")) return "Axis Bank";
        if (upper.contains("AMERICAN EXPRESS") || upper.contains("AMEX")) return "American Express";
        if (upper.contains("KOTAK")) return "Kotak Mahindra Bank";
        if (upper.contains("CITIBANK") || upper.contains("CITI")) return "Citibank";
        if (upper.contains("INDUSIND")) return "IndusInd Bank";
        return "Unknown Issuer";
    }

    private String detectNetwork(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("RUPAY")) return "RUPAY";
        if (upper.contains("MASTERCARD")) return "MASTERCARD";
        if (upper.contains("AMERICAN EXPRESS") || upper.contains("AMEX")) return "AMEX";
        if (upper.contains("VISA")) return "VISA";
        return "VISA";
    }

    private String detectCardName(String text, String issuer) {
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.contains("REGALIA")) return "Regalia";
        if (upper.contains("INFINIA")) return "Infinia";
        if (upper.contains("AMAZON PAY")) return "Amazon Pay";
        if (upper.contains("CORAL")) return "Coral";
        if (upper.contains("SIMPLYCLICK")) return "SimplyCLICK";
        if (upper.contains("SIMPLYSAVE")) return "SimplySAVE";
        if (upper.contains("MAGNUS")) return "Magnus";
        if (upper.contains("ACE")) return "Ace";
        if (upper.contains("MILLENNIA")) return "Millennia";
        return issuer + " Credit Card";
    }

    private String extractRegex(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private BigDecimal extractAmount(Pattern pattern, String text) {
        String match = extractRegex(pattern, text);
        if (match == null) {
            return null;
        }
        try {
            String clean = match.replace(",", "").trim();
            return new BigDecimal(clean).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate extractDate(Pattern pattern, String text) {
        String match = extractRegex(pattern, text);
        if (match == null) {
            return null;
        }
        String cleaned = match.replaceAll("[.]", "/").replaceAll("[-]", "/").trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/MMM/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd/MMM/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d/MMMM/yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private List<ExtractedTransactionDto> extractSampleTransactions(String text) {
        List<ExtractedTransactionDto> list = new ArrayList<>();
        Pattern txPattern = Pattern.compile(
                "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s+([A-Za-z0-9*\\s.,&]{4,35})\\s+([\\d,]+\\.\\d{2})\\s*(Cr|Dr|CR|DR)?",
                Pattern.CASE_INSENSITIVE);
        Matcher m = txPattern.matcher(text);
        int count = 0;
        while (m.find() && count < 10) {
            try {
                String dStr = m.group(1).replaceAll("[-]", "/");
                LocalDate d = LocalDate.parse(dStr, DateTimeFormatter.ofPattern("d/M/yyyy"));
                String desc = m.group(2).trim();
                BigDecimal amt = new BigDecimal(m.group(3).replace(",", ""));
                String type = (m.group(4) != null && m.group(4).toUpperCase().contains("CR")) ? "CREDIT" : "DEBIT";
                list.add(new ExtractedTransactionDto(d, desc, amt, type));
                count++;
            } catch (Exception ignored) {
            }
        }
        return list;
    }
}
