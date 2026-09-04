package com.pulsewallet.pulsewallet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.pulsewallet.pulsewallet.entity.FraudAlert;
import com.pulsewallet.pulsewallet.entity.Notification;
import com.pulsewallet.pulsewallet.entity.NotificationType;
import com.pulsewallet.pulsewallet.entity.Transaction;
import com.pulsewallet.pulsewallet.entity.TransactionType;
import com.pulsewallet.pulsewallet.entity.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("null")
class NotificationRepositoryTest {

        @Autowired
        private NotificationRepository notificationRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TransactionRepository transactionRepository;

        @Autowired
        private FraudAlertRepository fraudAlertRepository;

        @Test
        void savesAndRetrievesNotificationsScopedToUser() {
                User alice = userRepository.save(new User("Alice", "alice@example.com", "hash"));
                User bob = userRepository.save(new User("Bob", "bob@example.com", "hash"));

                notificationRepository.saveAndFlush(
                                new Notification(alice, "Warning", "Your card looks risky.",
                                                NotificationType.FRAUD_ALERT));
                Notification read = notificationRepository.saveAndFlush(
                                new Notification(alice, "Reminder", "Review your budget.", NotificationType.BUDGET));
                read.setRead(true);

                Notification otherUser = notificationRepository.saveAndFlush(
                                new Notification(bob, "Private", "Another user's alert.",
                                                NotificationType.FRAUD_ALERT));

                assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(alice.getId())).hasSize(2);
                assertThat(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(alice.getId()))
                                .extracting(Notification::getTitle)
                                .containsExactly("Warning");
                assertThat(notificationRepository.findByIdAndUserId(read.getId(), alice.getId())).isPresent();
                assertThat(notificationRepository.findByIdAndUserId(otherUser.getId(), alice.getId())).isEmpty();
                assertThat(notificationRepository.countByUserIdAndReadFalse(alice.getId())).isEqualTo(1L);
                assertThat(notificationRepository.findByUserIdOrderByCreatedAtDesc(bob.getId()))
                                .extracting(Notification::getTitle)
                                .containsExactly("Private");
        }

        @Test
        void storesFraudAlertNotificationsWithAUniqueFraudAlertLink() {
                User alice = userRepository.save(new User("Alice", "alice@example.com", "hash"));
                Transaction transaction = transactionRepository.saveAndFlush(new Transaction(
                                alice,
                                new BigDecimal("149.62"),
                                "Suspicious purchase",
                                "Coffee House",
                                "CARD",
                                null,
                                TransactionType.EXPENSE,
                                java.time.LocalDate.now()));

                FraudAlert alert = fraudAlertRepository.saveAndFlush(new FraudAlert(
                                transaction,
                                alice,
                                new BigDecimal("0.92"),
                                new BigDecimal("92.0"),
                                FraudAlert.RiskLevel.CRITICAL));

                Notification notification = notificationRepository.saveAndFlush(
                                new Notification(alice, "Suspicious transaction detected",
                                                "A potentially fraudulent transaction was detected.",
                                                NotificationType.FRAUD_ALERT, transaction, alert));

                assertThat(notification.getType()).isEqualTo(NotificationType.FRAUD_ALERT);
                assertThat(notification.getFraudAlert()).isNotNull();
                assertThat(notificationRepository.existsByFraudAlertId(alert.getId())).isTrue();
        }
}
