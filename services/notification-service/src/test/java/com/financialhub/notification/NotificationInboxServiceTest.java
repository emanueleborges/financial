package com.financialhub.notification;

import com.financialhub.notification.application.NotificationInboxService;
import com.financialhub.notification.domain.NotificationRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationInboxServiceTest {

    @Autowired
    NotificationInboxService inbox;

    @Test
    void doesNotDuplicateSameEventAndEmail() {
        inbox.saveIfAbsent("evt-1", "alice@test.com", "52998224725", "ok");
        inbox.saveIfAbsent("evt-1", "alice@test.com", "52998224725", "ok");
        inbox.saveIfAbsent("evt-1", "bob@test.com", "39053344705", "received");

        List<NotificationRecord> alice = inbox.listByDocument("52998224725");
        List<NotificationRecord> bob = inbox.listByDocument("39053344705");

        assertThat(alice).hasSize(1);
        assertThat(bob).hasSize(1);
    }
}
