package no.muda.jackbox;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.Method;

import no.muda.jackbox.example.ExampleDependency;
import no.muda.jackbox.example.ExampleRecordedObject;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JackboxRecordingTest {

    private static ClassLoader originalClassLoader;

    private ExampleRecordedObject recordedObject = new ExampleRecordedObject();

    @BeforeClass
    public static void setupClassloader() {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        // TODO: Runtime weaving!!!
        //Thread.currentThread().setContextClassLoader(new RecordingClassLoader());
    }

    @AfterClass
    public static void resetClassloader() {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
    }

    @Before
    public void startRecording() {
        JackboxRecorder.startRecording();
    }

    @Test
    public void shouldRecordMethodCall() throws Exception {
        int actualReturnedValue = recordedObject.exampleMethod(2, 3);

        MethodRecording recording = JackboxRecorder.getLastCompletedRecording();

        assertExampleMethodCall(2, 3, actualReturnedValue, recording);
    }

    @Test
    public void shouldRecordDependency() throws Exception {
        ExampleDependency exampleDependency = new ExampleDependency();
        recordedObject.setDependency(exampleDependency);

        String delegatedArgument = "abcd";
        recordedObject.exampleMethodThatDelegatesToDependency(delegatedArgument);

        MethodRecording recording = JackboxRecorder.getLastCompletedRecording();

        Method invokedMethodOnDependency = ExampleDependency.class.getMethod("invokedMethodOnDependency", String.class);
        MethodRecording dependentRecording =
            recording.getDependencyMethodRecordings(invokedMethodOnDependency)[0];

        assertThat(dependentRecording.getArguments()).containsOnly(delegatedArgument);
        assertThat(dependentRecording.getRecordedResult()).isEqualTo("ABCD");
    }

    @Test
    public void shouldRecordMultipleMethodCalls() throws Exception {
        int actualReturnedValue = recordedObject.exampleMethod(2, 3);
        int actualReturnedValue2 = recordedObject.exampleMethod(4, 5);
        MethodRecording[] recordings = JackboxRecorder.getAllRecordings();

        assertExampleMethodCall(2, 3, actualReturnedValue, recordings[0]);
        assertExampleMethodCall(4, 5, actualReturnedValue2, recordings[1]);
    }

    private void assertExampleMethodCall(int param1, int param2, int actualReturnedValue,
            MethodRecording recording) {
        assertThat(recording.getMethod().getName()).isEqualTo("exampleMethod");
        assertThat(recording.getArguments()).containsOnly(param1, param2);
        assertThat(recording.getRecordedResult()).isEqualTo(actualReturnedValue);
    }
}
