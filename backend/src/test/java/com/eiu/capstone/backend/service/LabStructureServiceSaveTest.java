package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.RubricCacheInvalidationSupport;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Field;
import com.eiu.capstone.backend.model.FieldDeclaration;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ClassRelationRepository;
import com.eiu.capstone.backend.repository.ConstructorDeclarationRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldDeclarationRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.repository.MethodDeclarationRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.repository.TermRepository;

@ExtendWith(MockitoExtension.class)
class LabStructureServiceSaveTest {

    @Mock private LabRepository labRepository;
    @Mock private TermRepository termRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private ClassEntityRepository classEntityRepository;
    @Mock private ClassRelationRepository classRelationRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private MethodRepository methodRepository;
    @Mock private ConstructorRepository constructorRepository;
    @Mock private ParameterRepository parameterRepository;
    @Mock private FieldDeclarationRepository fieldDeclarationRepository;
    @Mock private MethodDeclarationRepository methodDeclarationRepository;
    @Mock private ConstructorDeclarationRepository constructorDeclarationRepository;
    @Mock private MasterDataRepository masterDataRepository;
    @Mock private LabRubricCache labRubricCache;

    private RubricCacheInvalidationSupport rubricCacheInvalidationSupport;

    private LabStructureService labStructureService;

    @Mock private Lab lab;
    @Mock private Term term;
    @Mock private MasterData scope;

    private UUID labId;
    private UUID termId;

    @BeforeEach
    void setUp() {
        rubricCacheInvalidationSupport = new RubricCacheInvalidationSupport(labRubricCache);
        labStructureService = new LabStructureService(
                labRepository,
                termRepository,
                challengeRepository,
                classEntityRepository,
                classRelationRepository,
                fieldRepository,
                methodRepository,
                constructorRepository,
                parameterRepository,
                fieldDeclarationRepository,
                methodDeclarationRepository,
                constructorDeclarationRepository,
                masterDataRepository,
                rubricCacheInvalidationSupport);

        labId = UUID.randomUUID();
        termId = UUID.randomUUID();
        when(lab.getId()).thenReturn(labId);
        when(lab.getName()).thenReturn("Lab 2");
        when(lab.getTerm()).thenReturn(term);
        when(term.getId()).thenReturn(termId);

        when(scope.getId()).thenReturn(1);
        when(scope.getName()).thenReturn("PUBLIC");
    }

    @Test
    void saveLabStructure_clientClassUuid_createsClassWithThatId() {
        UUID challengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        ChallengeStructureDTO challengeDto = new ChallengeStructureDTO(
                challengeId,
                "Car Class",
                1,
                List.of(new ClassStructureDTO(
                        classId,
                        "Car",
                        1,
                        2,
                        false,
                        List.of(),
                        List.of(),
                        List.of())),
                List.of());
        LabStructureResponse payload = new LabStructureResponse(labId, "Lab 2", termId, List.of(challengeDto));

        Challenge challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setLab(lab);
        challenge.setName("Car Class");
        challenge.setChallengeNumber(1);

        when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId)).thenReturn(List.of());
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.empty());
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(List.of());
        when(classEntityRepository.findById(classId)).thenReturn(Optional.empty());
        when(masterDataRepository.findById(1)).thenReturn(Optional.of(scope));
        when(masterDataRepository.findById(2)).thenReturn(Optional.of(scope));
        when(classEntityRepository.save(any(ClassEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fieldRepository.findByClassEntity_Id(classId)).thenReturn(List.of());
        when(methodRepository.findByClassEntity_Id(classId)).thenReturn(List.of());
        when(constructorRepository.findByClassEntity_Id(classId)).thenReturn(List.of());
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId)).thenReturn(List.of(challenge));
        when(classEntityRepository.findByChallengeInWithAttributes(any())).thenReturn(List.of());

        labStructureService.saveLabStructure(labId, payload);

        ArgumentCaptor<ClassEntity> classCaptor = ArgumentCaptor.forClass(ClassEntity.class);
        verify(classEntityRepository).save(classCaptor.capture());
        assertEquals(classId, classCaptor.getValue().getId());
    }

    @Test
    void saveLabStructure_blankFieldName_throwsBadRequest() {
        UUID challengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        FieldStructureDTO fieldDto = new FieldStructureDTO(fieldId, "   ", "int", 1);
        ClassStructureDTO classDto = new ClassStructureDTO(
                classId, "Car", 1, 2, false, List.of(fieldDto), List.of(), List.of());
        ChallengeStructureDTO challengeDto = new ChallengeStructureDTO(
                challengeId, "Problem", 1, List.of(classDto), List.of());
        LabStructureResponse payload = new LabStructureResponse(labId, "Lab 2", termId, List.of(challengeDto));

        Challenge challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setLab(lab);

        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(classId);
        classEntity.setChallenge(challenge);

        when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId)).thenReturn(List.of());
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(List.of());
        when(classEntityRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(masterDataRepository.findById(1)).thenReturn(Optional.of(scope));
        when(masterDataRepository.findById(2)).thenReturn(Optional.of(scope));
        when(classEntityRepository.save(any(ClassEntity.class))).thenReturn(classEntity);
        when(fieldRepository.findByClassEntity_Id(classId)).thenReturn(List.of());
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> labStructureService.saveLabStructure(labId, payload));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void saveLabStructure_classIdFromOtherChallenge_throwsBadRequest() {
        UUID challengeId = UUID.randomUUID();
        UUID otherChallengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();

        Challenge challenge = new Challenge();
        challenge.setId(challengeId);
        challenge.setLab(lab);

        Challenge otherChallenge = new Challenge();
        otherChallenge.setId(otherChallengeId);
        otherChallenge.setLab(lab);

        ClassEntity existingClass = new ClassEntity();
        existingClass.setId(classId);
        existingClass.setChallenge(otherChallenge);

        ClassStructureDTO classDto = new ClassStructureDTO(
                classId, "Car", 1, 2, false, List.of(), List.of(), List.of());
        ChallengeStructureDTO challengeDto = new ChallengeStructureDTO(
                challengeId, "Problem", 1, List.of(classDto), List.of());
        LabStructureResponse payload = new LabStructureResponse(labId, "Lab 2", termId, List.of(challengeDto));

        when(labRepository.findById(labId)).thenReturn(Optional.of(lab));
        when(challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId)).thenReturn(List.of());
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(challengeRepository.save(any(Challenge.class))).thenReturn(challenge);
        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(List.of());
        when(classEntityRepository.findById(classId)).thenReturn(Optional.of(existingClass));
        when(masterDataRepository.findById(1)).thenReturn(Optional.of(scope));
        when(masterDataRepository.findById(2)).thenReturn(Optional.of(scope));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> labStructureService.saveLabStructure(labId, payload));
        assertEquals(400, ex.getStatusCode().value());
    }
}
