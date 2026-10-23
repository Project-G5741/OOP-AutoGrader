package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.UserDTO;
import com.eiu.capstone.backend.model.Role;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabDeadlineEmailSentRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.repository.PasswordResetTokenRepository;
import com.eiu.capstone.backend.repository.RoleRepository;
import com.eiu.capstone.backend.repository.StudentLabProgressRepository;
import com.eiu.capstone.backend.repository.SubmissionChallengeResultRepository;
import com.eiu.capstone.backend.repository.SubmissionConstructorResultRepository;
import com.eiu.capstone.backend.repository.SubmissionFieldResultRepository;
import com.eiu.capstone.backend.repository.SubmissionMethodResultRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismFingerprintRepository;
import com.eiu.capstone.backend.repository.SubmissionPlagiarismMatchRepository;
import com.eiu.capstone.backend.repository.SubmissionRelationResultRepository;
import com.eiu.capstone.backend.repository.SubmissionTestcaseResultRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.UserAccountRepository;
import com.eiu.capstone.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LabSubmissionRepository labSubmissionRepository;

    @Mock
    private SubmissionPlagiarismMatchRepository plagiarismMatchRepository;

    @Mock
    private SubmissionPlagiarismFingerprintRepository plagiarismFingerprintRepository;

    @Mock
    private SubmissionFieldResultRepository submissionFieldResultRepository;

    @Mock
    private SubmissionMethodResultRepository submissionMethodResultRepository;

    @Mock
    private SubmissionConstructorResultRepository submissionConstructorResultRepository;

    @Mock
    private SubmissionChallengeResultRepository submissionChallengeResultRepository;

    @Mock
    private SubmissionRelationResultRepository submissionRelationResultRepository;

    @Mock
    private SubmissionTestcaseResultRepository submissionTestcaseResultRepository;

    @Mock
    private StudentLabProgressRepository studentLabProgressRepository;

    @Mock
    private TermEnrollmentRepository termEnrollmentRepository;

    @Mock
    private LabDeadlineEmailSentRepository labDeadlineEmailSentRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private SessionValidityService sessionValidityService;

    @Mock
    private TermService termService;

    @Mock
    private UserAccount existingUser;

    private UserService userService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                roleRepository,
                passwordEncoder,
                labSubmissionRepository,
                plagiarismMatchRepository,
                plagiarismFingerprintRepository,
                submissionFieldResultRepository,
                submissionMethodResultRepository,
                submissionConstructorResultRepository,
                submissionChallengeResultRepository,
                submissionRelationResultRepository,
                submissionTestcaseResultRepository,
                studentLabProgressRepository,
                termEnrollmentRepository,
                labDeadlineEmailSentRepository,
                passwordResetTokenRepository,
                sessionValidityService,
                termService);
        userId = UUID.randomUUID();
        when(existingUser.getId()).thenReturn(userId);
        when(existingUser.getEmail()).thenReturn("student@eiu.edu.vn");
        when(existingUser.getFullName()).thenReturn("Student One");
        when(existingUser.getPasswordHash()).thenReturn("old-hash");
        when(existingUser.getStudentCode()).thenReturn("111");
        when(existingUser.getRoles()).thenReturn(new HashSet<>(Set.of(new Role("STUDENT"))));
        when(existingUser.getIsActive()).thenReturn(true);
    }

    @Test
    void updateUser_dualRole_persistsBothCodes() {
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Dual User");
        request.setEmail("dual@eiu.edu.vn");
        request.setRoleNames(Set.of("STUDENT", "LECTURER"));
        request.setStudentCode("111");
        request.setTeacherCode("222");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));
        when(roleRepository.findByName("LECTURER")).thenReturn(Optional.of(new Role("LECTURER")));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, request);

        verify(existingUser).setStudentCode("111");
        verify(existingUser).setTeacherCode("222");
    }

    @Test
    void updateUser_studentOnly_clearsTeacherCode() {
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Student One");
        request.setEmail("student@eiu.edu.vn");
        request.setRoleNames(Set.of("STUDENT"));
        request.setStudentCode("111");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, request);

        verify(existingUser).setTeacherCode(null);
    }

    @Test
    void updateUser_withPassword_encodesNewHash() {
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Student One");
        request.setEmail("student@eiu.edu.vn");
        request.setRoleNames(Set.of("STUDENT"));
        request.setStudentCode("111");
        request.setPassword("newpass123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));
        when(passwordEncoder.encode("newpass123")).thenReturn("encoded");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, request);

        verify(passwordEncoder).encode("newpass123");
        verify(existingUser).setPasswordHash("encoded");
    }

    @Test
    void updateUser_blankPassword_keepsExistingHash() {
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Student One");
        request.setEmail("student@eiu.edu.vn");
        request.setRoleNames(Set.of("STUDENT"));
        request.setStudentCode("111");
        request.setPassword("");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, request);

        verify(passwordEncoder, never()).encode(any());
        verify(existingUser, never()).setPasswordHash(any());
    }

    @Test
    void updateUser_legacyRoleAndIrn_stillWorks() {
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Student One");
        request.setEmail("student@eiu.edu.vn");
        request.setRole("STUDENT");
        request.setIrn("333");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, request);

        verify(existingUser).setStudentCode("333");
    }

    @Test
    void updateUser_emptyRoleNames_throws400() {
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Student One");
        request.setEmail("student@eiu.edu.vn");
        request.setRoleNames(Set.of());

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUser(userId, request));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void deleteUser_hardDeletesUserAndRelatedData() {
        UserAccount student = studentAccount();
        when(userRepository.findAllWithRolesByIdIn(List.of(userId))).thenReturn(List.of(student));

        UserDTO.UserResponse result = userService.deleteUser(userId);

        assertEquals("student@eiu.edu.vn", result.getEmail());
        verify(studentLabProgressRepository).deleteByUser_Id(userId);
        verify(termEnrollmentRepository).deleteByUser_Id(userId);
        verify(labDeadlineEmailSentRepository).deleteByUser_Id(userId);
        verify(passwordResetTokenRepository).deleteByUser_Id(userId);
        verify(plagiarismMatchRepository).deleteInvolvingUser(userId);
        verify(plagiarismFingerprintRepository).deleteAllByUserId(userId);
        verify(submissionTestcaseResultRepository).deleteAssertionResultsByUserId(userId);
        verify(submissionTestcaseResultRepository).deleteByUserId(userId);
        verify(submissionFieldResultRepository).deleteByUserId(userId);
        verify(submissionMethodResultRepository).deleteByUserId(userId);
        verify(submissionConstructorResultRepository).deleteByUserId(userId);
        verify(submissionChallengeResultRepository).deleteByUserId(userId);
        verify(submissionRelationResultRepository).deleteByUserId(userId);
        verify(labSubmissionRepository).deleteByUser_Id(userId);
        verify(labSubmissionRepository, never()).findByUser_Id(any());
        verify(submissionFieldResultRepository, never()).deleteBySubmission(any());
        verify(submissionMethodResultRepository, never()).deleteBySubmission(any());
        verify(submissionConstructorResultRepository, never()).deleteBySubmission(any());
        verify(submissionChallengeResultRepository, never()).deleteBySubmission(any());
        verify(submissionRelationResultRepository, never()).deleteBySubmission(any());
        verify(submissionTestcaseResultRepository, never()).deleteBySubmission_Id(any());
        verify(userRepository).delete(student);
        verify(sessionValidityService).invalidateCachedEmail("student@eiu.edu.vn");
    }

    @Test
    void deleteUser_notFound_throws404() {
        when(userRepository.findAllWithRolesByIdIn(List.of(userId))).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.deleteUser(userId));
        assertEquals(404, ex.getStatusCode().value());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void suspendStudent_setsInactive() {
        UserAccount student = studentAccount();
        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        UserDTO.UserResponse result = userService.suspendStudent(userId);

        assertFalse(result.isActive());
        assertFalse(student.getIsActive());
        verify(sessionValidityService).bumpSessionVersion(student);
    }

    @Test
    void restoreStudent_setsActive() {
        UserAccount student = studentAccount();
        student.setIsActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(userRepository.save(student)).thenReturn(student);

        UserDTO.UserResponse result = userService.restoreStudent(userId);

        assertTrue(result.isActive());
        assertTrue(student.getIsActive());
        verify(sessionValidityService, never()).bumpSessionVersion(any(UserAccount.class));
    }

    @Test
    void suspendStudent_lecturer_throws400() {
        UserAccount lecturer = studentAccount();
        lecturer.setRoles(Set.of(new Role("LECTURER")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(lecturer));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.suspendStudent(userId));
        assertEquals(400, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void suspendStudent_dualRole_throws400() {
        UserAccount dual = studentAccount();
        dual.setRoles(Set.of(new Role("STUDENT"), new Role("LECTURER")));
        when(userRepository.findById(userId)).thenReturn(Optional.of(dual));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.suspendStudent(userId));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void getAllUser_setsCurrentQuarterWhenActiveAndEnrolled() {
        UUID enrolledId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UserAccount enrolled = studentAccount();
        setUserId(enrolled, enrolledId);
        UserAccount notEnrolled = studentAccount();
        notEnrolled.setEmail("other@eiu.edu.vn");
        notEnrolled.setStudentCode("222");
        setUserId(notEnrolled, otherId);

        when(userRepository.findAllWithRoles()).thenReturn(List.of(enrolled, notEnrolled));
        when(termService.currentTermLabel()).thenReturn(Optional.of("2026 — Quarter 1"));
        when(termEnrollmentRepository.findUserIdsEnrolledInCurrentTerm()).thenReturn(List.of(enrolledId));

        List<UserDTO.UserResponse> rows = userService.getAllUser();

        assertEquals(2, rows.size());
        assertEquals("2026 — Quarter 1", rows.get(0).getCurrentQuarter());
        assertNull(rows.get(1).getCurrentQuarter());
    }

    @Test
    void getAllUser_blanksCurrentQuarterWhenInactiveEvenIfEnrolled() {
        UUID enrolledId = UUID.randomUUID();
        UserAccount inactive = studentAccount();
        inactive.setIsActive(false);
        setUserId(inactive, enrolledId);

        when(userRepository.findAllWithRoles()).thenReturn(List.of(inactive));
        when(termService.currentTermLabel()).thenReturn(Optional.of("2026 — Quarter 1"));
        when(termEnrollmentRepository.findUserIdsEnrolledInCurrentTerm()).thenReturn(List.of(enrolledId));

        List<UserDTO.UserResponse> rows = userService.getAllUser();

        assertEquals(1, rows.size());
        assertNull(rows.get(0).getCurrentQuarter());
    }

    @Test
    void getAllUser_blanksCurrentQuarterWhenNoCurrentTerm() {
        UserAccount student = studentAccount();
        setUserId(student, UUID.randomUUID());

        when(userRepository.findAllWithRoles()).thenReturn(List.of(student));
        when(termService.currentTermLabel()).thenReturn(Optional.empty());

        List<UserDTO.UserResponse> rows = userService.getAllUser();

        assertEquals(1, rows.size());
        assertNull(rows.get(0).getCurrentQuarter());
        verify(termEnrollmentRepository, never()).findUserIdsEnrolledInCurrentTerm();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUser_lecturerCannotGrantAdmin() {
        signInAs("LECTURER");
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Student One");
        request.setEmail("student@eiu.edu.vn");
        request.setRoleNames(Set.of("STUDENT", "ADMIN"));
        request.setStudentCode("111");
        request.setTeacherCode("222");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.updateUser(userId, request));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void updateUser_lecturerSaveKeepsExistingAdmin() {
        when(existingUser.getRoles()).thenReturn(new HashSet<>(Set.of(new Role("ADMIN"), new Role("LECTURER"))));
        when(existingUser.getTeacherCode()).thenReturn("222");
        signInAs("LECTURER");
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@eiu.edu.vn");
        request.setRoleNames(Set.of("LECTURER"));
        request.setTeacherCode("222");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("LECTURER")).thenReturn(Optional.of(new Role("LECTURER")));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(new Role("ADMIN")));
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(userId, request);

        verify(existingUser).setRoles(org.mockito.ArgumentMatchers.argThat(roles ->
                roles.stream().anyMatch(role -> "ADMIN".equals(role.getName()))
                        && roles.stream().anyMatch(role -> "LECTURER".equals(role.getName()))));
    }

    @Test
    void updateUser_adminCanClearRolesAndKeepsTeacherCode() {
        when(existingUser.getRoles()).thenReturn(new HashSet<>(Set.of(new Role("ADMIN"))));
        when(existingUser.getTeacherCode()).thenReturn("222");
        signInAs("ADMIN");
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Former Admin");
        request.setEmail("admin@eiu.edu.vn");
        request.setRoleNames(Set.of());
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.updateUser(userId, request);

        verify(existingUser).setRoles(org.mockito.ArgumentMatchers.argThat(Set::isEmpty));
        verify(existingUser, never()).setTeacherCode(null);
        verify(sessionValidityService).bumpSessionVersion(existingUser);
    }

    @Test
    void updateUser_adminWithoutLecturerKeepsTeacherCode() {
        when(existingUser.getRoles()).thenReturn(new HashSet<>(Set.of(new Role("STUDENT"))));
        signInAs("ADMIN");
        UserDTO.UpdateUserRequest request = new UserDTO.UpdateUserRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@eiu.edu.vn");
        request.setRoleNames(Set.of("ADMIN", "STUDENT"));
        request.setTeacherCode("222");
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(new Role("ADMIN")));
        when(roleRepository.findByName("STUDENT")).thenReturn(Optional.of(new Role("STUDENT")));

        userService.updateUser(userId, request);

        verify(existingUser).setTeacherCode("222");
        verify(existingUser).setStudentCode(null);
    }

    @Test
    void deleteUser_lecturerCannotDeleteAdmin() {
        UserAccount admin = studentAccount();
        admin.setRoles(new HashSet<>(Set.of(new Role("ADMIN"))));
        signInAs("LECTURER");
        when(userRepository.findAllWithRolesByIdIn(List.of(userId))).thenReturn(List.of(admin));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.deleteUser(userId));
        assertEquals(403, ex.getStatusCode().value());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void suspendStudent_rejectsAdmin() {
        when(existingUser.getRoles()).thenReturn(new HashSet<>(Set.of(new Role("ADMIN"), new Role("STUDENT"))));
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.suspendStudent(userId));
        assertEquals(400, ex.getStatusCode().value());
    }

    private void signInAs(String role) {
        JwtUserPrincipal principal = new JwtUserPrincipal("actor@eiu.edu.vn", "T1", List.of(role));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private UserAccount studentAccount() {
        UserAccount student = new UserAccount();
        student.setFullName("Student One");
        student.setEmail("student@eiu.edu.vn");
        student.setPasswordHash("hash");
        student.setStudentCode("111");
        student.setIsActive(true);
        student.setRoles(new HashSet<>(Set.of(new Role("STUDENT"))));
        return student;
    }

    private static void setUserId(UserAccount user, UUID id) {
        try {
            var field = UserAccount.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
