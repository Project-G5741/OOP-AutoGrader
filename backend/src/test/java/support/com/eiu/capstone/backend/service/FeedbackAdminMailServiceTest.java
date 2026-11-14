package support.com.eiu.capstone.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eiu.capstone.backend.model.FeedbackItem;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.service.FeedbackAdminMailService;
import com.eiu.capstone.backend.service.TransactionalEmailSender;

@ExtendWith(MockitoExtension.class)
class FeedbackAdminMailServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TransactionalEmailSender emailSender;

    private FeedbackAdminMailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new FeedbackAdminMailService(userAccountRepository, emailSender);
    }

    @Test
    void sendsSubjectAndMessageToEachActiveAdmin() {
        when(userAccountRepository.findActiveAdmins()).thenReturn(List.of(
                admin("one@eiu.edu.vn"),
                admin("two@eiu.edu.vn"),
                admin("  ")));
        FeedbackItem item = item("BUG", "Nguyen Van A", "2331200001", "Please add dark mode");

        mailService.notifyAdmins(item);

        String expectedBody = """
                Type: BUG
                Author: Nguyen Van A (2331200001)

                Please add dark mode
                """.trim();
        verify(emailSender).sendPlainText(
                "one@eiu.edu.vn", FeedbackAdminMailService.SUBJECT, expectedBody);
        verify(emailSender).sendPlainText(
                "two@eiu.edu.vn", FeedbackAdminMailService.SUBJECT, expectedBody);
    }

    @Test
    void continuesWhenOneAdminSendFails() {
        when(userAccountRepository.findActiveAdmins()).thenReturn(List.of(
                admin("a@eiu.edu.vn"),
                admin("b@eiu.edu.vn")));
        doThrow(new RuntimeException("smtp down"))
                .when(emailSender).sendPlainText(eq("a@eiu.edu.vn"), any(), any());

        mailService.notifyAdmins(item("CONTRIBUTION", "Lecturer B", "T001", "Idea"));

        verify(emailSender).sendPlainText(eq("b@eiu.edu.vn"), eq(FeedbackAdminMailService.SUBJECT), any());
    }

    @Test
    void skipsWhenNoAdmins() {
        when(userAccountRepository.findActiveAdmins()).thenReturn(List.of());
        mailService.notifyAdmins(item("BUG", "A", "1", "Hello"));
        verify(emailSender, never()).sendPlainText(any(), any(), any());
    }

    private static UserAccount admin(String email) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        return user;
    }

    private static FeedbackItem item(String type, String name, String irn, String message) {
        FeedbackItem item = new FeedbackItem();
        item.setItemType(type);
        item.setAuthorName(name);
        item.setAuthorIrn(irn);
        item.setMessage(message);
        return item;
    }
}
