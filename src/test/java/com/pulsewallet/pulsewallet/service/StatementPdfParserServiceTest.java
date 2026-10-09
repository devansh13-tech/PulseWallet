package com.pulsewallet.pulsewallet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.pulsewallet.pulsewallet.dto.CreditCardResponse;
import com.pulsewallet.pulsewallet.dto.StatementConfirmationRequest;
import com.pulsewallet.pulsewallet.dto.StatementExtractionResponse;
import com.pulsewallet.pulsewallet.entity.CreditCard;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.CreditCardRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.service.statement.StatementPdfParserService;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
class StatementPdfParserServiceTest {

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private StatementPdfParserService parserService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestEntities.withId(new User("Alice", "alice@example.com", "hash"), 1L);
    }

    private byte[] createSamplePdf(String content) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(content);
                stream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void parseStatementPdfExtractsKeyFields() throws IOException {
        String statementText = "HDFC Bank Credit Card Statement Ending in 5678 Total Amount Due: INR 25,000.00 Minimum Due: 1,250.00 Total Credit Limit: 100,000.00 Payment Due Date: 15/10/2026 Statement Date: 01/10/2026";
        byte[] pdfBytes = createSamplePdf(statementText);
        MockMultipartFile file = new MockMultipartFile("file", "hdfc_statement.pdf", "application/pdf", pdfBytes);

        StatementExtractionResponse response = parserService.parseStatementPdf(file);

        assertNotNull(response);
        assertEquals("HDFC Bank", response.issuer());
        assertEquals("5678", response.lastFourDigits());
        assertEquals(new BigDecimal("25000.00"), response.statementBalance());
        assertEquals(new BigDecimal("1250.00"), response.minimumDue());
        assertEquals(new BigDecimal("100000.00"), response.creditLimit());
        assertEquals(LocalDate.of(2026, 10, 15), response.dueDate());
        assertEquals(LocalDate.of(2026, 10, 1), response.statementDate());
        assertTrue(response.requiresUserConfirmation());
        assertFalse(response.isEncrypted());
        assertFalse(response.isScanned());
    }

    @Test
    void rejectsNonPdfFile() {
        byte[] notPdf = "This is a plain text file, not a PDF".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "fake.pdf", "application/pdf", notPdf);

        assertThrows(IllegalArgumentException.class, () -> parserService.parseStatementPdf(file));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> parserService.parseStatementPdf(file));
    }

    @Test
    void confirmAndSaveCreatesNewCard() {
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        CreditCard saved = new CreditCard(user, "HDFC Bank", "Regalia", "VISA", "5678",
                new BigDecimal("100000"), new BigDecimal("25000"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                BigDecimal.ZERO, "Cashback", "Lounge");
        saved.setDataSource("STATEMENT_UPLOAD");
        saved.setStatementBalance(new BigDecimal("25000"));
        saved.setMinimumDue(new BigDecimal("1250"));
        TestEntities.withId(saved, 10L);

        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(saved);

        StatementConfirmationRequest request = new StatementConfirmationRequest(
                null, "HDFC Bank", "Regalia", "VISA", "5678",
                new BigDecimal("100000"), new BigDecimal("25000"),
                new BigDecimal("25000"), new BigDecimal("1250"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                "Cashback", "Lounge");

        CreditCardResponse response = parserService.confirmAndSave(1L, request);

        assertNotNull(response);
        assertEquals("HDFC Bank", response.issuer());
        assertEquals("5678", response.lastFourDigits());
        assertEquals("STATEMENT_UPLOAD", response.dataSource());
    }

    @Test
    void confirmAndSaveUpdatesExistingCard() {
        CreditCard existing = new CreditCard(user, "HDFC Bank", "Regalia", "VISA", "5678",
                new BigDecimal("100000"), new BigDecimal("10000"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15),
                BigDecimal.ZERO, "Cashback", "Lounge");
        TestEntities.withId(existing, 10L);

        when(creditCardRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(existing));

        StatementConfirmationRequest request = new StatementConfirmationRequest(
                10L, "HDFC Bank", "Regalia", "VISA", "5678",
                new BigDecimal("120000"), new BigDecimal("30000"),
                new BigDecimal("30000"), new BigDecimal("1500"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                "Updated Cashback", "Updated Lounge");

        CreditCardResponse response = parserService.confirmAndSave(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("120000"), response.creditLimit());
        assertEquals(new BigDecimal("30000"), response.currentOutstanding());
        assertEquals("STATEMENT_UPLOAD", response.dataSource());
    }

    @Test
    void confirmAndSaveThrowsWhenCardNotOwned() {
        when(creditCardRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        StatementConfirmationRequest request = new StatementConfirmationRequest(
                99L, "HDFC Bank", "Regalia", "VISA", "5678",
                new BigDecimal("100000"), new BigDecimal("25000"),
                new BigDecimal("25000"), new BigDecimal("1250"),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15),
                null, null);

        assertThrows(ResourceNotFoundException.class, () -> parserService.confirmAndSave(1L, request));
    }
}
