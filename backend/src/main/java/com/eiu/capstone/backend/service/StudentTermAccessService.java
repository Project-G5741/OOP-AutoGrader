package com.eiu.capstone.backend.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;

@Service
public class StudentTermAccessService {

    private final TermService termService;
    private final LabDeadlineHelper labDeadlineHelper;

    public StudentTermAccessService(TermService termService, LabDeadlineHelper labDeadlineHelper) {
        this.termService = termService;
        this.labDeadlineHelper = labDeadlineHelper;
    }

    public boolean isInCurrentTerm(UserAccount user) {
        if (user == null || !user.getIsActive()) {
            return false;
        }
        return termService.isInCurrentTerm(user.getId());
    }

    public void requireCanSubmit(UserAccount user, Lab lab) {
        requireStudentLabAccess(user, lab);
    }

    public void requireStudentLabAccess(UserAccount user, Lab lab) {
        if (user == null || !user.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is inactive");
        }
        Term current = termService.findCurrentTerm()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "No current quarter is set"));
        if (!termService.isEnrolled(user.getId(), current.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You are not enrolled in the current quarter");
        }
        if (lab == null || lab.getTerm() == null || !current.getId().equals(lab.getTerm().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This lab is not in the current quarter");
        }
        requireLabOpenForStudent(lab);
    }

    public void requireLabOpenForStudent(Lab lab) {
        if (!labDeadlineHelper.isOpenForStudentSubmission(
                lab.isStudentVisible(), lab.getReleaseDate(), Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This lab is not available for submission yet");
        }
    }
}
