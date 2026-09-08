package support.com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.service.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;
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
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
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
        ChallengeCompileErrors.none(),
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
  void buildClassDataFromRubric_usesStudentShellTypeAndWarningWhenShellPartiallyMatches() {
    UUID challengeId = UUID.randomUUID();
    UUID classId = UUID.randomUUID();
    UUID methodId = UUID.randomUUID();

    ClassRubric classRubric = new ClassRubric(
        classId,
        "CakeFactory",
        "PUBLIC",
        "CLASS",
        true,
        List.of(),
        List.of(new MethodRubric(methodId, "createCake", "PUBLIC", "Cake", false, true, false, List.of())),
        List.of());
    ChallengeRubric challengeRubric = new ChallengeRubric(
        challengeId, 1, "Challenge 1", List.of(classRubric), List.of());

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

    List<ClassDetailDTO> result = service.buildClassDataFromRubric(
        challengeRubric,
        new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
        ChallengeCompileErrors.none(),
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

  @Test
  void buildClassDataFromRubric_heritageMismatch_errorsShellAndGatesMembersWithoutHeritageCopy() {
    UUID challengeId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();
    UUID observerId = UUID.randomUUID();
    UUID methodId = UUID.randomUUID();

    ClassRubric subscriber = new ClassRubric(
        subscriberId,
        "EmailSubscriber",
        "PUBLIC",
        "CLASS",
        false,
        List.of(),
        List.of(new MethodRubric(methodId, "update", "PUBLIC", "void", false, false, false, List.of("String"))),
        List.of());
    ClassRubric observer = new ClassRubric(
        observerId, "Observer", "PUBLIC", "INTERFACE", false, List.of(), List.of(), List.of());
    ChallengeRubric challengeRubric = new ChallengeRubric(
        challengeId,
        1,
        "Observer",
        List.of(subscriber, observer),
        List.of(new RelationRubric(
            UUID.randomUUID(), subscriberId, "EmailSubscriber", observerId, "Observer", "realization")));

    ClassShellEntry shell = matchingPublicClassShell();
    shell.interfaceSimpleNames = List.of();

    ClassSnapshot classSnapshot = new ClassSnapshot();
    classSnapshot.shells.put(subscriberId.toString(), shell);
    ClassMethodEntry methodEntry = new ClassMethodEntry();
    methodEntry.name = "update";
    methodEntry.scope = "public";
    methodEntry.returnType = "void";
    classSnapshot.methods.put(methodId.toString(), methodEntry);

    ParsedSubmissionSnapshot.ChallengeSnapshot snapshot = new ParsedSubmissionSnapshot.ChallengeSnapshot();
    snapshot.classSnapshot = classSnapshot;

    List<ClassDetailDTO> result = service.buildClassDataFromRubric(
        challengeRubric,
        new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
        ChallengeCompileErrors.none(),
        snapshot);

    ClassDetailDTO card = result.stream()
        .filter(item -> "EmailSubscriber".equals(item.name()))
        .findFirst()
        .orElseThrow();
    assertEquals("error", card.status());
    assertEquals(null, card.error());
    assertEquals(false, card.methods().get(0).ok());
  }

  @Test
  void buildClassDataFromRubric_heritageMatch_keepsShellSuccess() {
    UUID challengeId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();
    UUID observerId = UUID.randomUUID();
    UUID methodId = UUID.randomUUID();

    ClassRubric subscriber = new ClassRubric(
        subscriberId,
        "EmailSubscriber",
        "PUBLIC",
        "CLASS",
        false,
        List.of(),
        List.of(new MethodRubric(methodId, "update", "PUBLIC", "void", false, false, false, List.of("String"))),
        List.of());
    ClassRubric observer = new ClassRubric(
        observerId, "Observer", "PUBLIC", "INTERFACE", false, List.of(), List.of(), List.of());
    ChallengeRubric challengeRubric = new ChallengeRubric(
        challengeId,
        1,
        "Observer",
        List.of(subscriber, observer),
        List.of(new RelationRubric(
            UUID.randomUUID(), subscriberId, "EmailSubscriber", observerId, "Observer", "realization")));

    ClassShellEntry shell = matchingPublicClassShell();
    shell.interfaceSimpleNames = List.of("Observer");

    ClassSnapshot classSnapshot = new ClassSnapshot();
    classSnapshot.shells.put(subscriberId.toString(), shell);
    ClassMethodEntry methodEntry = new ClassMethodEntry();
    methodEntry.name = "update";
    methodEntry.scope = "public";
    methodEntry.returnType = "void";
    classSnapshot.methods.put(methodId.toString(), methodEntry);

    ParsedSubmissionSnapshot.ChallengeSnapshot snapshot = new ParsedSubmissionSnapshot.ChallengeSnapshot();
    snapshot.classSnapshot = classSnapshot;

    List<ClassDetailDTO> result = service.buildClassDataFromRubric(
        challengeRubric,
        new SubmissionCorrectIds(Set.of(), Set.of(methodId), Set.of(), Set.of()),
        ChallengeCompileErrors.none(),
        snapshot);

    ClassDetailDTO card = result.stream()
        .filter(item -> "EmailSubscriber".equals(item.name()))
        .findFirst()
        .orElseThrow();
    assertEquals("success", card.status());
    assertEquals(true, card.methods().get(0).ok());
  }

  @Test
  void buildClassDataFromRubric_noHeritagePair_doesNotFailShell() {
    UUID challengeId = UUID.randomUUID();
    UUID subscriberId = UUID.randomUUID();
    UUID methodId = UUID.randomUUID();

    ClassRubric subscriber = new ClassRubric(
        subscriberId,
        "EmailSubscriber",
        "PUBLIC",
        "CLASS",
        false,
        List.of(),
        List.of(new MethodRubric(methodId, "update", "PUBLIC", "void", false, false, false, List.of("String"))),
        List.of());
    ChallengeRubric challengeRubric = new ChallengeRubric(
        challengeId, 1, "Observer", List.of(subscriber), List.of());

    ClassShellEntry shell = matchingPublicClassShell();
    shell.interfaceSimpleNames = List.of();

    ClassSnapshot classSnapshot = new ClassSnapshot();
    classSnapshot.shells.put(subscriberId.toString(), shell);
    ClassMethodEntry methodEntry = new ClassMethodEntry();
    methodEntry.name = "update";
    methodEntry.scope = "public";
    methodEntry.returnType = "void";
    classSnapshot.methods.put(methodId.toString(), methodEntry);

    ParsedSubmissionSnapshot.ChallengeSnapshot snapshot = new ParsedSubmissionSnapshot.ChallengeSnapshot();
    snapshot.classSnapshot = classSnapshot;

    List<ClassDetailDTO> result = service.buildClassDataFromRubric(
        challengeRubric,
        new SubmissionCorrectIds(Set.of(), Set.of(methodId), Set.of(), Set.of()),
        ChallengeCompileErrors.none(),
        snapshot);

    assertEquals("success", result.get(0).status());
    assertEquals(true, result.get(0).methods().get(0).ok());
  }

  @Test
  void buildClassDataFromRubric_perClassCompileError_gatesOnlyFailedCard() {
    UUID goodId = UUID.randomUUID();
    UUID badId = UUID.randomUUID();
    ChallengeRubric challengeRubric = new ChallengeRubric(
        UUID.randomUUID(),
        1,
        "Challenge 1",
        List.of(
            new ClassRubric(goodId, "Good", "PUBLIC", "CLASS", false, List.of(), List.of(), List.of()),
            new ClassRubric(badId, "Bad", "PUBLIC", "CLASS", false, List.of(), List.of(), List.of())),
        List.of(),
        List.of());

    List<ClassDetailDTO> result = service.buildClassDataFromRubric(
        challengeRubric,
        new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
        ChallengeCompileErrors.perClass(Map.of("Bad", "ERROR: line 1: ';' expected")),
        null);

    ClassDetailDTO good = result.stream().filter(card -> "Good".equals(card.name())).findFirst().orElseThrow();
    ClassDetailDTO bad = result.stream().filter(card -> "Bad".equals(card.name())).findFirst().orElseThrow();
    assertEquals(null, good.error());
    assertEquals("ERROR: line 1: ';' expected", bad.error());
    assertEquals("error", bad.status());
  }

  @Test
  void buildClassDataFromRubric_catastrophicCompileError_gatesAllCards() {
    UUID goodId = UUID.randomUUID();
    UUID badId = UUID.randomUUID();
    ChallengeRubric challengeRubric = new ChallengeRubric(
        UUID.randomUUID(),
        1,
        "Challenge 1",
        List.of(
            new ClassRubric(goodId, "Good", "PUBLIC", "CLASS", false, List.of(), List.of(), List.of()),
            new ClassRubric(badId, "Bad", "PUBLIC", "CLASS", false, List.of(), List.of(), List.of())),
        List.of(),
        List.of());

    List<ClassDetailDTO> result = service.buildClassDataFromRubric(
        challengeRubric,
        new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
        ChallengeCompileErrors.catastrophic("Failed to count class files"),
        null);

    assertEquals(2, result.size());
    for (ClassDetailDTO card : result) {
      assertEquals("Failed to count class files", card.error());
      assertEquals("error", card.status());
    }
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
        ChallengeCompileErrors.none(),
        snapshot);

    assertEquals(1, result.size());
    return result.get(0);
  }

  private static ClassShellEntry matchingPublicClassShell() {
    ClassShellEntry shell = new ClassShellEntry();
    shell.scope = "public";
    shell.declaringType = "class";
    shell.isAbstract = false;
    shell.isStatic = false;
    return shell;
  }

  private static MasterData masterData(int id, String name) {
    MasterData masterData = new MasterData();
    masterData.setId(id);
    masterData.setName(name);
    return masterData;
  }
}
