package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
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
import com.eiu.capstone.backend.repository.RoleRepository;
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
    private UserAccount existingUser;

    private UserService userService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
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
