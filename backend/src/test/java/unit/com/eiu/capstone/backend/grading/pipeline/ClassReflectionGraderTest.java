package unit.com.eiu.capstone.backend.grading.pipeline;

import com.eiu.capstone.backend.grading.pipeline.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.ParsedClass;
import com.eiu.capstone.backend.grading.ParsedConstructor;
import com.eiu.capstone.backend.grading.ParsedMethod;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;

class ClassReflectionGraderTest {

    private final ClassReflectionGrader grader = new ClassReflectionGrader();

    @Test
    void explicitPrivateNoArgConstructorPassesWhenRubricIsNotDefault() {
        UUID ctorId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Logger",
                List.of(new ClassRubric(
                        classId,
                        "Logger",
                        "public",
                        "CLASS",
                        false,
                        List.of(),
                        List.of(),
                        List.of(new ConstructorRubric(ctorId, "private", false, List.of())))),
                List.of(),
                List.of());

        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = "Logger";
        parsed.scope = "public";
        parsed.declaringType = "CLASS";
        parsed.isAbstract = false;
        parsed.fields = List.of();
        parsed.methods = List.of();
        ParsedConstructor ctor = new ParsedConstructor();
        ctor.scope = "private";
        ctor.parameterTypes = List.of();
        parsed.constructors = List.of(ctor);

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(parsed)));

        assertTrue(result.constructors().stream()
                .filter(entry -> entry.constructorId().equals(ctorId))
                .findFirst()
                .orElseThrow()
                .correct());
    }

    @Test
    void nestedClassMatchesByQualifiedName() {
        UUID penId = UUID.randomUUID();
        UUID builderId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Pen challenge",
                List.of(
                        new ClassRubric(penId, "Pen", "public", "CLASS", false, List.of(), List.of(), List.of()),
                        new ClassRubric(builderId, "PenBuilder", "Pen", "public", "CLASS", false, true,
                                List.of(), List.of(), List.of(), 1)),
                List.of(),
                List.of());

        ParsedClass pen = new ParsedClass();
        pen.simpleName = "Pen";
        pen.scope = "public";
        pen.declaringType = "class";
        pen.isAbstract = false;
        pen.fields = List.of();
        pen.methods = List.of();
        pen.constructors = List.of();

        ParsedClass builder = new ParsedClass();
        builder.simpleName = "PenBuilder";
        builder.outerSimpleName = "Pen";
        builder.scope = "public";
        builder.declaringType = "class";
        builder.isAbstract = false;
        builder.isStatic = true;
        builder.fields = List.of();
        builder.methods = List.of();
        builder.constructors = List.of();

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(pen, builder)));

        assertTrue(result.constructors().isEmpty());
        assertEquals(0, result.fields().size());
        assertEquals(0, result.methods().size());
    }

    @Test
    void wrongClassShellZerosMemberCreditEvenWhenMethodMatches() {
        UUID classId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Factory Method",
                List.of(new ClassRubric(
                        classId,
                        "CakeFactory",
                        "public",
                        "CLASS",
                        true,
                        List.of(),
                        List.of(new MethodRubric(
                                methodId,
                                "createCake",
                                "public",
                                "Cake",
                                false,
                                true,
                                false,
                                List.of())),
                        List.of())),
                List.of(),
                List.of());

        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = "CakeFactory";
        parsed.scope = "public";
        parsed.declaringType = "interface";
        parsed.isAbstract = false;
        parsed.fields = List.of();
        ParsedMethod method = new ParsedMethod();
        method.name = "createCake";
        method.scope = "public";
        method.returnType = "Cake";
        method.isStatic = false;
        method.isAbstract = true;
        method.isFinal = false;
        method.parameterTypes = List.of();
        parsed.methods = List.of(method);
        parsed.constructors = List.of();

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(parsed)));

        assertEquals(false, result.methods().stream()
                .filter(entry -> entry.methodId().equals(methodId))
                .findFirst()
                .orElseThrow()
                .correct());
    }

    @Test
    void implementsObserver_shellPassesWhenDeclared() {
        ObserverFixture fx = observerFixture("realization");
        ParsedClass parsed = subscriber("EmailSubscriber", null, List.of("Observer"), true);

        ClassReflectionGrader.ClassPillarResult result = grade(fx, parsed);

        assertTrue(methodCorrect(result, fx.methodId));
    }

    @Test
    void missingImplementsObserver_zerosMembersEvenWhenUpdateMatches() {
        ObserverFixture fx = observerFixture("realization");
        ParsedClass parsed = subscriber("EmailSubscriber", null, List.of(), true);

        ClassReflectionGrader.ClassPillarResult result = grade(fx, parsed);

        assertFalse(methodCorrect(result, fx.methodId));
    }

    @Test
    void extendsAnimal_shellPassesWhenSuperclassMatches() {
        UUID animalId = UUID.randomUUID();
        UUID dogId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Animals",
                List.of(
                        classShell(animalId, "Animal"),
                        classShell(dogId, "Dog")),
                List.of(new RelationRubric(
                        UUID.randomUUID(), dogId, "Dog", animalId, "Animal", "inheritance")));

        ParsedClass dog = new ParsedClass();
        dog.simpleName = "Dog";
        dog.scope = "public";
        dog.declaringType = "class";
        dog.isAbstract = false;
        dog.superclassSimpleName = "Animal";
        dog.interfaceSimpleNames = List.of();
        dog.fields = List.of();
        dog.methods = List.of();
        dog.constructors = List.of();

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(dog)));

        assertTrue(result.methods().isEmpty());
        assertEquals(0, result.fields().size());
    }

    @Test
    void inheritedImplementsThroughParent_doesNotPassDeclaredClause() {
        ObserverFixture fx = observerFixture("realization");
        ParsedClass parsed = subscriber("EmailSubscriber", "BaseSubscriber", List.of(), true);

        ClassReflectionGrader.ClassPillarResult result = grade(fx, parsed);

        assertFalse(methodCorrect(result, fx.methodId));
    }

    @Test
    void extraSerializable_doesNotFailRequiredImplements() {
        ObserverFixture fx = observerFixture("realization");
        ParsedClass parsed = subscriber("EmailSubscriber", null, List.of("Observer", "Serializable"), true);

        ClassReflectionGrader.ClassPillarResult result = grade(fx, parsed);

        assertTrue(methodCorrect(result, fx.methodId));
    }

    @Test
    void noHeritageRow_doesNotFailForMissingImplements() {
        UUID classId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Observer",
                List.of(classWithUpdate(classId, methodId, "EmailSubscriber")),
                List.of());
        ParsedClass parsed = subscriber("EmailSubscriber", null, List.of(), true);

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(parsed)));

        assertTrue(methodCorrect(result, methodId));
    }

    @Test
    void hasMmdFalse_stillAppliesJavaHeritageCheck() {
        ObserverFixture fx = observerFixture("realization");
        ChallengeRubric rubric = new ChallengeRubric(
                fx.rubric.challengeId(),
                fx.rubric.challengeNumber(),
                fx.rubric.name(),
                fx.rubric.classes(),
                fx.rubric.relations(),
                List.of(),
                false);
        ParsedClass parsed = subscriber("EmailSubscriber", null, List.of(), true);

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(parsed)));

        assertFalse(methodCorrect(result, fx.methodId));
    }

    @Test
    void extraHeritageRows_skipHeritageCheck() {
        ObserverFixture fx = observerFixture("realization");
        UUID loggerId = UUID.randomUUID();
        List<RelationRubric> extras = List.of(
                fx.rubric.relations().get(0),
                new RelationRubric(
                        UUID.randomUUID(), fx.subscriberId, "EmailSubscriber", loggerId, "Logger", "realization"));
        ChallengeRubric rubric = new ChallengeRubric(
                fx.rubric.challengeId(),
                fx.rubric.challengeNumber(),
                fx.rubric.name(),
                fx.rubric.classes(),
                extras);
        ParsedClass parsed = subscriber("EmailSubscriber", null, List.of(), true);

        ClassReflectionGrader.ClassPillarResult result = grader.grade(
                ChallengeGradingContext.of(rubric, null, null, List.of(parsed)));

        assertTrue(methodCorrect(result, fx.methodId));
    }

    @Test
    void wrongKind_extendsWhenImplementsRequired_failsShell() {
        ObserverFixture fx = observerFixture("realization");
        ParsedClass parsed = subscriber("EmailSubscriber", "Observer", List.of(), true);

        ClassReflectionGrader.ClassPillarResult result = grade(fx, parsed);

        assertFalse(methodCorrect(result, fx.methodId));
    }

    private ClassReflectionGrader.ClassPillarResult grade(ObserverFixture fx, ParsedClass parsed) {
        return grader.grade(ChallengeGradingContext.of(fx.rubric, null, null, List.of(parsed)));
    }

    private boolean methodCorrect(ClassReflectionGrader.ClassPillarResult result, UUID methodId) {
        return result.methods().stream()
                .filter(entry -> entry.methodId().equals(methodId))
                .findFirst()
                .orElseThrow()
                .correct();
    }

    private ObserverFixture observerFixture(String relationTypeName) {
        UUID subscriberId = UUID.randomUUID();
        UUID observerId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Observer",
                List.of(
                        classWithUpdate(subscriberId, methodId, "EmailSubscriber"),
                        classShell(observerId, "Observer")),
                List.of(new RelationRubric(
                        UUID.randomUUID(),
                        subscriberId,
                        "EmailSubscriber",
                        observerId,
                        "Observer",
                        relationTypeName)));
        return new ObserverFixture(rubric, subscriberId, methodId);
    }

    private ClassRubric classShell(UUID id, String name) {
        return new ClassRubric(id, name, "public", "CLASS", false, List.of(), List.of(), List.of());
    }

    private ClassRubric classWithUpdate(UUID classId, UUID methodId, String name) {
        return new ClassRubric(
                classId,
                name,
                "public",
                "CLASS",
                false,
                List.of(),
                List.of(new MethodRubric(
                        methodId,
                        "update",
                        "public",
                        "void",
                        false,
                        false,
                        false,
                        List.of("String"))),
                List.of());
    }

    private ParsedClass subscriber(
            String simpleName, String superclassSimpleName, List<String> interfaces, boolean withUpdate) {
        ParsedClass parsed = new ParsedClass();
        parsed.simpleName = simpleName;
        parsed.scope = "public";
        parsed.declaringType = "class";
        parsed.isAbstract = false;
        parsed.superclassSimpleName = superclassSimpleName;
        parsed.interfaceSimpleNames = interfaces;
        parsed.fields = List.of();
        ParsedMethod method = new ParsedMethod();
        method.name = "update";
        method.scope = "public";
        method.returnType = "void";
        method.isStatic = false;
        method.isAbstract = false;
        method.isFinal = false;
        method.parameterTypes = List.of("String");
        parsed.methods = withUpdate ? List.of(method) : List.of();
        parsed.constructors = List.of();
        return parsed;
    }

    private record ObserverFixture(ChallengeRubric rubric, UUID subscriberId, UUID methodId) {}
}
