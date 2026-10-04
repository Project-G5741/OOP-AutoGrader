package unit.com.eiu.capstone.backend.grading.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.testcase.InvocationErrorMessage;

class InvocationErrorMessageTest {

    @Test
    void formatsJvmConstructorSignature() {
        assertEquals(
                "Your Car class does not have a constructor with parameters (int, String).",
                InvocationErrorMessage.forStudent("Car.<init>(int,java.lang.String)"));
    }

    @Test
    void formatsJvmMethodSignature() {
        assertEquals(
                "Your Car class does not have a method accelerate().",
                InvocationErrorMessage.forStudent("Car.accelerate()"));
    }

    @Test
    void formatsUnknownNamedInstance() {
        assertEquals(
                "This step uses object \"myCar\", but it was not created in an earlier step.",
                InvocationErrorMessage.forStudent("Unknown named instance: myCar"));
    }

    @Test
    void leavesFriendlyMessagesUntouched() {
        assertEquals("Stdout mismatch", InvocationErrorMessage.forStudent("Stdout mismatch"));
    }
}
