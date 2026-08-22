package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassMethodEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassShellEntry;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassSnapshot;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.MethodDeclaration;

class ClassStructureServiceShellDisplayTest {

  private ClassStructureService service;

  @BeforeEach
  void setUp() {
    service = new ClassStructureService(
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
  }

  @Test
  void buildClassData_usesStudentShellTypeAndWarningWhenShellPartiallyMatches() {
    UUID challengeId = UUID.randomUUID();
    UUID classId = UUID.randomUUID();
    UUID methodId = UUID.randomUUID();

    MasterData publicScope = masterData(1, "PUBLIC");
    MasterData classType = masterData(2, "CLASS");

    ClassEntity classEntity = new ClassEntity();
    classEntity.setId(classId);
    classEntity.setName("CakeFactory");
    classEntity.setScope(publicScope);
    classEntity.setDeclaringType(classType);
    classEntity.setAbstract(true);
    classEntity.setWeight(1);

    Challenge challenge = new Challenge();
    challenge.setId(challengeId);
    classEntity.setChallenge(challenge);

    Method method = new Method();
    method.setId(methodId);
    method.setName("createCake");
    MethodDeclaration declaration = new MethodDeclaration();
    declaration.setScope(publicScope);
    declaration.setReturnType("Cake");
    declaration.setStatic(false);
    declaration.setAbstract(true);
    declaration.setFinal(false);
    method.setMethodDeclaration(declaration);
    method.setClassEntity(classEntity);

    LabChallengeStructureBundle structure = new LabChallengeStructureBundle(
        Map.of(1, "PUBLIC", 2, "CLASS"),
        Map.of(challengeId, List.of(classEntity)),
        Map.of(),
        Map.of(classId, List.of(method)),
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of());

    ClassShellEntry shell = new ClassShellEntry();
    shell.scope = "public";
    shell.declaringType = "interface";
    shell.isAbstract = false;
    shell.isStatic = false;

    ClassSnapshot classSnapshot = new ClassSnapshot();
    classSnapshot.shells.put(classId.toString(), shell);

    ClassMethodEntry methodEntry = new ClassMethodEntry();
    methodEntry.name = "createCake";
    methodEntry.scope = "public";
    methodEntry.returnType = "Cake";
    methodEntry.isStatic = false;
    methodEntry.isAbstract = true;
    methodEntry.isFinal = false;
    classSnapshot.methods.put(methodId.toString(), methodEntry);

    ParsedSubmissionSnapshot.ChallengeSnapshot snapshot = new ParsedSubmissionSnapshot.ChallengeSnapshot();
    snapshot.classSnapshot = classSnapshot;

    List<ClassDetailDTO> result = service.buildClassData(
        structure,
        challengeId,
        new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
        null,
        snapshot);

    assertEquals(1, result.size());
    ClassDetailDTO cakeFactory = result.get(0);
    assertEquals("INTERFACE", cakeFactory.type());
    assertEquals("error", cakeFactory.status());
    assertEquals(1, cakeFactory.methods().size());
    assertEquals(false, cakeFactory.methods().get(0).ok());
    assertEquals(false, cakeFactory.methods().get(0).partial());
  }

  @Test
  void buildClassData_memberlessEnumWithMatchingShell_isSuccess() {
    ClassDetailDTO flowerType = buildMemberlessEnum("enum");
    assertEquals("ENUM", flowerType.type());
    assertEquals("success", flowerType.status());
    assertEquals(0, flowerType.fields().size());
    assertEquals(0, flowerType.constructors().size());
    assertEquals(0, flowerType.methods().size());
  }

  @Test
  void buildClassData_memberlessEnumWithWrongDeclaringType_isError() {
    ClassDetailDTO flowerType = buildMemberlessEnum("class");
    assertEquals("CLASS", flowerType.type());
    assertEquals("error", flowerType.status());
  }

  private ClassDetailDTO buildMemberlessEnum(String studentDeclaringType) {
    UUID challengeId = UUID.randomUUID();
    UUID classId = UUID.randomUUID();

    MasterData publicScope = masterData(1, "PUBLIC");
    MasterData enumType = masterData(3, "ENUM");

    ClassEntity classEntity = new ClassEntity();
    classEntity.setId(classId);
    classEntity.setName("FlowerType");
    classEntity.setScope(publicScope);
    classEntity.setDeclaringType(enumType);
    classEntity.setAbstract(false);
    classEntity.setWeight(1);

    Challenge challenge = new Challenge();
    challenge.setId(challengeId);
    classEntity.setChallenge(challenge);

    LabChallengeStructureBundle structure = new LabChallengeStructureBundle(
        Map.of(1, "PUBLIC", 3, "ENUM"),
        Map.of(challengeId, List.of(classEntity)),
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of());

    ClassShellEntry shell = new ClassShellEntry();
    shell.scope = "public";
    shell.declaringType = studentDeclaringType;
    shell.isAbstract = false;
    shell.isStatic = false;

    ClassSnapshot classSnapshot = new ClassSnapshot();
    classSnapshot.shells.put(classId.toString(), shell);

    ParsedSubmissionSnapshot.ChallengeSnapshot snapshot = new ParsedSubmissionSnapshot.ChallengeSnapshot();
    snapshot.classSnapshot = classSnapshot;

    List<ClassDetailDTO> result = service.buildClassData(
        structure,
        challengeId,
        new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
        null,
        snapshot);

    assertEquals(1, result.size());
    return result.get(0);
  }

  private static MasterData masterData(int id, String name) {
    MasterData masterData = new MasterData();
    masterData.setId(id);
    masterData.setName(name);
    return masterData;
  }
}
