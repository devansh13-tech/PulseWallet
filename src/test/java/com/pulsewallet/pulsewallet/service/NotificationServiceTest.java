package com.pulsewallet.pulsewallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pulsewallet.pulsewallet.dto.NotificationResponse;
import com.pulsewallet.pulsewallet.entity.Notification;
import com.pulsewallet.pulsewallet.entity.User;
import com.pulsewallet.pulsewallet.exception.ResourceNotFoundException;
import com.pulsewallet.pulsewallet.repository.NotificationRepository;
import com.pulsewallet.pulsewallet.repository.UserRepository;
import com.pulsewallet.pulsewallet.support.TestEntities;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class NotificationServiceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;
    private User owner;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);
        owner = TestEntities.withId(new User("Ada", "ada@example.com", "hash"), OWNER_ID);
    }

    @Test
    void create_createsNotificationForUser() {
        Notification saved = TestEntities.withId(
                new Notification(owner, "Suspicious transaction detected",
                        "A potentially fraudulent transaction of ₹149.62 was detected.", "FRAUD_ALERT"),
                10L);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponse response = notificationService.create(OWNER_ID, "Suspicious transaction detected",
                "A potentially fraudulent transaction of ₹149.62 was detected.", "FRAUD_ALERT");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("Suspicious transaction detected");
        assertThat(response.type()).isEqualTo("FRAUD_ALERT");
    }

    @Test
    void list_returnsUserNotifications() {
        Notification first = TestEntities.withId(new Notification(owner, "A", "One", "FRAUD_ALERT"), 1L);
        Notification second = TestEntities.withId(new Notification(owner, "B", "Two", "BUDGET"), 2L);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(first, second));

        assertThat(notificationService.list(OWNER_ID)).extracting(NotificationResponse::title)
                .containsExactly("A", "B");
    }

    @Test
    void listUnread_returnsOnlyUnreadNotifications() {
        Notification unread = TestEntities.withId(new Notification(owner, "Unread", "Open", "FRAUD_ALERT"), 5L);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(OWNER_ID)).thenReturn(List.of(unread));

        assertThat(notificationService.listUnread(OWNER_ID)).extracting(NotificationResponse::title)
                .containsExactly("Unread");
    }

    @Test
    void unreadCount_returnsCountForThisUser() {
        when(notificationRepository.countByUserIdAndReadFalse(OWNER_ID)).thenReturn(3L);

        assertThat(notificationService.unreadCount(OWNER_ID)).isEqualTo(3L);
    }

    @Test
    void markRead_marksNotificationAsReadForOwner() {
        Notification notification = TestEntities.withId(new Notification(owner, "One", "Details", "FRAUD_ALERT"), 7L);
        when(notificationRepository.findByIdAndUserId(7L, OWNER_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markRead(7L, OWNER_ID);

        assertThat(response.read()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @Test
    void markRead_rejectsOwnershipMismatch() {
        when(notificationRepository.findByIdAndUserId(7L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(7L, OWNER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
