package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
                passwordResetTokenRepository);
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
        when(labSubmissionRepository.findByUser_Id(userId)).thenReturn(List.of());

        UserDTO.UserResponse result = userService.deleteUser(userId);

        assertEquals("student@eiu.edu.vn", result.getEmail());
        verify(studentLabProgressRepository).deleteByUser_Id(userId);
        verify(plagiarismFingerprintRepository, never()).deleteAllByUserId(any());
        verify(termEnrollmentRepository).deleteByUser_Id(userId);
        verify(labDeadlineEmailSentRepository).deleteByUser_Id(userId);
        verify(passwordResetTokenRepository).deleteByUser_Id(userId);
        verify(userRepository).delete(student);
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
}
