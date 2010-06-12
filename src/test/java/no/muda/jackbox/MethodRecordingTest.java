package no.muda.jackbox;

import no.muda.jackbox.example.ExampleDependency;
import no.muda.jackbox.example.ExampleRecordedObject;
import no.muda.jackbox.example.classannotation.ClassAnnotationDemoService;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.fest.assertions.Assertions.assertThat;

public class MethodRecordingTest {
    private Method method;
    private MethodRecording recording;

    private MethodRecording dependencyMethodCall;
    private Method dependencyMethod1;
    private MethodRecording dependencyMethodCall2;
    private Method dependencyMethod2;

    private MethodRecording demoMethodCall;
    private Method demoMethod1;

    @Before
    public void methodSetup() throws NoSuchMethodException {
        method = ExampleRecordedObject.class.getMethod("exampleMethod", Integer.TYPE, Integer.TYPE);

        recording = new MethodRecording(ExampleRecordedObject.class, method, new Object[] { 5, 6 });
        recording.setReturnValue(11);

        dependencyMethod1 = ExampleDependency.class.getMethod("invokedMethodOnDependency", String.class);
        dependencyMethodCall = new MethodRecording(ExampleDependency.class, dependencyMethod1, new Object[] {"test"});
        dependencyMethodCall.setReturnValue("testreturn");

        dependencyMethod2 = ExampleDependency.class.getMethod("anotherTestMethodWithoutArgumentsOrReturnValue");
        dependencyMethodCall2 = new MethodRecording(ExampleDependency.class, dependencyMethod2, new Object[] {});

        demoMethod1 = ClassAnnotationDemoService.class.getMethod("doSomething");
        demoMethodCall = new MethodRecording(ClassAnnotationDemoService.class, demoMethod1, new Object[]{});
    }

    @Test
    public void shouldUseHashCodeOfRecordedMethod() throws NoSuchMethodException {
        assertThat(recording.hashCode()).isEqualTo(method.hashCode());
    }

    @Test
    public void shouldHaveTheMostImportantValuesInItsToString() {
        assertThat(recording.toString())
                .contains("exampleMethod(int,int)")
                .contains("arguments=[5, 6]")
                .contains("returnValue=11");
    }

    @Test
    public void recordOneDependency() {
        recording.addDependencyMethodCall(dependencyMethodCall);

        DependencyRecording recorded = recording.getDependencyRecording(dependencyMethodCall.getTargetClass());
        assertThat(recorded.getMethodRecording(dependencyMethod1)).isEqualTo(dependencyMethodCall);
    }

    @Test
    public void recordTwoDependencyRecordingsOnDifferentClasses() {
        recording.addDependencyMethodCall(dependencyMethodCall);
        recording.addDependencyMethodCall(demoMethodCall);

        DependencyRecording recorded = recording.getDependencyRecording(dependencyMethodCall.getTargetClass());
        assertThat(recorded.getMethodRecording(dependencyMethod1)).isEqualTo(dependencyMethodCall);

        recorded = recording.getDependencyRecording(demoMethodCall.getTargetClass());
        assertThat(recorded.getMethodRecording(demoMethod1)).isEqualTo(demoMethodCall);
    }

    @Test
    public void recordTwoDependencyRecordingsOnSameClassDifferentMethods() {
        recording.addDependencyMethodCall(dependencyMethodCall);
        recording.addDependencyMethodCall(dependencyMethodCall2);

        DependencyRecording recorded = recording.getDependencyRecording(dependencyMethodCall.getTargetClass());
        assertThat(recorded.getMethodRecording(dependencyMethod1)).isEqualTo(dependencyMethodCall);

        recorded = recording.getDependencyRecording(dependencyMethodCall2.getTargetClass());
        assertThat(recorded.getMethodRecording(dependencyMethod2)).isEqualTo(dependencyMethodCall2);
    }
}
