package com.pulsewallet.pulsewallet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.pulsewallet.pulsewallet.entity.FraudAlert;
import com.pulsewallet.pulsewallet.entity.FraudAlert.RiskLevel;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FraudAlertRepositoryTest {

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndReadsAnAlertWithItsTransactionAndUser() {
        User user = userRepository.save(new User("Alert Test", "alert-test@example.com", "hash"));
        Transaction transaction = transactionRepository.save(new Transaction(
                user,
                new BigDecimal("149.62"),
                "Suspicious purchase",
                null,
                TransactionType.EXPENSE,
                LocalDate.of(2026, 8, 29)));

        FraudAlert saved = fraudAlertRepository.saveAndFlush(new FraudAlert(
                transaction,
                user,
                new BigDecimal("0.92000000"),
                new BigDecimal("92.00000000"),
                RiskLevel.CRITICAL));

        FraudAlert found = fraudAlertRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTransaction().getId()).isEqualTo(transaction.getId());
        assertThat(found.getUser().getId()).isEqualTo(user.getId());
        assertThat(found.getFraudProbability()).isEqualByComparingTo("0.92");
        assertThat(found.getRiskScore()).isEqualByComparingTo("92.00");
        assertThat(found.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(found.isResolved()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
    }
}
