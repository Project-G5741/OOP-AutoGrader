package support.com.eiu.capstone.backend.service.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.service.compile.RubricCompileSourceFilter;
import com.eiu.capstone.backend.service.compile.StudentSourceNormalizer.SourceEntry;

class RubricCompileSourceFilterTest {

    @Test
    void allowedSimpleNames_includesNestedOuter() {
        ClassRubric nested = new ClassRubric(
                UUID.randomUUID(), "Inner", "Outer", "public", "CLASS", false, true,
                List.of(), List.of(), List.of(), 1);
        ChallengeRubric challenge = new ChallengeRubric(
                UUID.randomUUID(), 1, "c1", List.of(nested), List.of(), List.of());

        Set<String> names = RubricCompileSourceFilter.allowedSimpleNames(challenge);
        assertTrue(names.contains("Inner"));
        assertTrue(names.contains("Outer"));
    }

    @Test
    void keepRubricSources_omitsApplicationAndKeepsFee() {
        List<SourceEntry> sources = List.of(
                new SourceEntry("Fee.java", "public class Fee {}"),
                new SourceEntry("Application.java", """
                        public class Application {
                          public static void main(String[] args) {
                            cart.add(new OrderedItem(new (RedRose(),3));
                          }
                        }
                        """));

        List<SourceEntry> kept = RubricCompileSourceFilter.keepRubricSources(
                sources, Set.of("Fee", "Tax"));

        assertEquals(1, kept.size());
        assertEquals("Fee.java", kept.get(0).logicalPath());
    }

    @Test
    void keepRubricSources_nullAllowed_keepsAll() {
        List<SourceEntry> sources = List.of(
                new SourceEntry("A.java", "public class A {}"),
                new SourceEntry("B.java", "public class B {}"));
        assertEquals(2, RubricCompileSourceFilter.keepRubricSources(sources, null).size());
    }

    @Test
    void keepRubricSources_emptyAllowed_keepsNone() {
        List<SourceEntry> sources = List.of(new SourceEntry("A.java", "public class A {}"));
        assertTrue(RubricCompileSourceFilter.keepRubricSources(sources, Set.of()).isEmpty());
    }
}
