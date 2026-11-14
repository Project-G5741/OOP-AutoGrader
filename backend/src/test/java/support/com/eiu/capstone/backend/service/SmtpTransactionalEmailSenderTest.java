package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.eiu.capstone.backend.service.SmtpTransactionalEmailSender;

@ExtendWith(MockitoExtension.class)
class SmtpTransactionalEmailSenderTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpTransactionalEmailSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpTransactionalEmailSender(mailSender, "from@eiu.edu.vn");
    }

    @Test
    void batchUsesOneSendForEveryRecipient() {
        int failed = sender.sendPlainTextEach(
                List.of("a@eiu.edu.vn", "b@eiu.edu.vn"), "Subject", "Body");

        assertEquals(0, failed);
        ArgumentCaptor<SimpleMailMessage[]> messages = ArgumentCaptor.forClass(SimpleMailMessage[].class);
        verify(mailSender).send(messages.capture());
        assertEquals(2, messages.getValue().length);
        assertEquals("a@eiu.edu.vn", messages.getValue()[0].getTo()[0]);
        assertEquals("b@eiu.edu.vn", messages.getValue()[1].getTo()[0]);
        assertEquals("from@eiu.edu.vn", messages.getValue()[0].getFrom());
    }

    @Test
    void batchCountsPartialFailures() {
        SimpleMailMessage failedMessage = new SimpleMailMessage();
        failedMessage.setTo("a@eiu.edu.vn");
        doThrow(new MailSendException(Map.of(failedMessage, new RuntimeException("rejected"))))
                .when(mailSender).send(any(SimpleMailMessage[].class));

        int failed = sender.sendPlainTextEach(
                List.of("a@eiu.edu.vn", "b@eiu.edu.vn"), "Subject", "Body");

        assertEquals(1, failed);
    }

    @Test
    void connectionFailureCountsEveryRecipient() {
        doThrow(new MailSendException("Mail server connection failed"))
                .when(mailSender).send(any(SimpleMailMessage[].class));

        int failed = sender.sendPlainTextEach(List.of("a@eiu.edu.vn", "b@eiu.edu.vn"), "Subject", "Body");

        assertEquals(2, failed);
    }
}
