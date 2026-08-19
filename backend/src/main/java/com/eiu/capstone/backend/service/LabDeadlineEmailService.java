package com.eiu.capstone.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabDeadlineEmailSent;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabDeadlineEmailSentRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;

@Service
public class LabDeadlineEmailService {

    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LabRepository labRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final UserAccountRepository userAccountRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final LabDeadlineEmailSentRepository emailSentRepository;
    private final TransactionalEmailSender emailSender;
    private final LabDeadlineHelper labDeadlineHelper;
    private final String frontendUrl;

    public LabDeadlineEmailService(LabRepository labRepository,
                                     TermEnrollmentRepository termEnrollmentRepository,
                                     UserAccountRepository userAccountRepository,
                                     LabSubmissionRepository labSubmissionRepository,
                                     LabDeadlineEmailSentRepository emailSentRepository,
                                     TransactionalEmailSender emailSender,
                                     LabDeadlineHelper labDeadlineHelper,
                                     @Value("${FRONTEND_URL:http://localhost:5173}") String frontendUrl) {
        this.labRepository = labRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.userAccountRepository = userAccountRepository;
        this.labSubmissionRepository = labSubmissionRepository;
        this.emailSentRepository = emailSentRepository;
        this.emailSender = emailSender;
        this.labDeadlineHelper = labDeadlineHelper;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public void processThreshold(short thresholdHours) {
        List<Lab> labs = labRepository.findAllWithDeadlineAndTerm();
        Instant now = Instant.now();
        for (Lab lab : labs) {
            Instant cutoff = labDeadlineHelper.cutoffInstant(lab.getDeadlineDate());
            Instant target = cutoff.minus(Duration.ofHours(thresholdHours));
            if (now.isBefore(target) || now.isAfter(target.plusSeconds(60))) {
                continue;
            }
            sendForLabThreshold(lab, thresholdHours);
        }
    }

    private void sendForLabThreshold(Lab lab, short thresholdHours) {
        List<java.util.UUID> studentIds =
                termEnrollmentRepository.findActiveStudentIdsByTermId(lab.getTerm().getId());
        if (studentIds.isEmpty()) {
            return;
        }
        List<UserAccount> students = userAccountRepository.findAllById(studentIds);
        for (UserAccount student : students) {
            if (student.getEmail() == null || student.getEmail().isBlank()) {
                continue;
            }
            if (labSubmissionRepository.countByUser_IdAndLab_Id(student.getId(), lab.getId()) > 0) {
                continue;
            }
            if (emailSentRepository.existsByLab_IdAndUser_IdAndThresholdHours(
                    lab.getId(), student.getId(), thresholdHours)) {
                continue;
            }
            String subject = thresholdHours == 72
                    ? "Lab deadline in 3 days: " + lab.getName()
                    : "Lab deadline tomorrow: " + lab.getName();
            String body = """
                    Hello,

                    Your lab "%s" is due on %s (23:59 Vietnam time).

                    Submit before the deadline if you want your score counted for lecturer grading views.

                    Student dashboard: %s/student-dashboard
                    """.formatted(lab.getName(), lab.getDeadlineDate().format(DEADLINE_FORMAT), frontendUrl);
            emailSender.sendPlainText(student.getEmail(), subject, body);

            LabDeadlineEmailSent sent = new LabDeadlineEmailSent();
            sent.setLab(lab);
            sent.setUser(student);
            sent.setThresholdHours(thresholdHours);
            sent.setSentAt(java.time.OffsetDateTime.now());
            emailSentRepository.save(sent);
        }
    }
}
