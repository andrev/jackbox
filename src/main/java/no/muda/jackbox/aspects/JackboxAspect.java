package no.muda.jackbox.aspects;

import java.lang.reflect.Method;

import no.muda.jackbox.DependencyRecording;
import no.muda.jackbox.JackboxRecorder;
import no.muda.jackbox.MethodRecording;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class JackboxAspect {

    private static ThreadLocal<MethodRecording> ongoingRecording = new ThreadLocal<MethodRecording>();
    private static boolean replayMode = false;
    private static ThreadLocal<MethodRecording> methodRecording = new ThreadLocal<MethodRecording>();

    @Around("call(@no.muda.jackbox.annotations.Recording * *(..)) " +
            "|| (execution(public * *(..)) && @within(no.muda.jackbox.annotations.Recording))")
    public Object captureRecordedClass(ProceedingJoinPoint thisPointCut) throws Throwable {
        MethodRecording recording = createMethodRecording(thisPointCut);

        if (ongoingRecording.get() != null) {
            throw new IllegalStateException("Don't want to override " + ongoingRecording.get().getMethod());
        }
        ongoingRecording.set(recording);
        try {
            Object result = thisPointCut.proceed();
            recording.setReturnValue(result);
        }
        catch (Throwable t) {
            recording.setExceptionThrown(t);
        }
        ongoingRecording.set(null);

        JackboxRecorder.addRecording(recording);

        if (recording.getExceptionThrown() != null) throw recording.getExceptionThrown();
        else return recording.getRecordedResult();
    }

    @Around("call(@no.muda.jackbox.annotations.Dependency * *(..)) " +
            "|| (execution(public * *(..)) && @within(no.muda.jackbox.annotations.Dependency))")
    public Object captureDependencies(ProceedingJoinPoint thisPointCut) throws Throwable {
        if (replayMode) {
            return capturedValue(thisPointCut);
        }

        MethodRecording methodRecording = createMethodRecording(thisPointCut);
        ongoingRecording.get().addDependencyMethodCall(methodRecording);

        try {
            Object result = thisPointCut.proceed();
            methodRecording.setReturnValue(result);
            return result;
        }
        catch (Throwable t) {
            methodRecording.setExceptionThrown(t);
            throw t;
        }
    }

    private Object capturedValue(ProceedingJoinPoint thisPointCut) {
        DependencyRecording dependencyRecording = methodRecording.get().getDependencyRecording(thisPointCut.getSignature().getDeclaringType());

        MethodRecording dependencyMethodRecording = dependencyRecording.getMethodRecordings(thisPointCut.getSignature().getName())[0];
        return dependencyMethodRecording.getRecordedResult();
    }

    @SuppressWarnings("unchecked")
    private MethodRecording createMethodRecording(ProceedingJoinPoint thisPointCut) {
        Class targetClass = thisPointCut.getSignature().getDeclaringType();

        Method method = getMethod(thisPointCut);

        Object[] arguments = thisPointCut.getArgs();
        return new MethodRecording(targetClass, method, arguments);
    }

    private Method getMethod(ProceedingJoinPoint thisPointCut) {
        Method method = ((MethodSignature) thisPointCut.getSignature()).getMethod();
        return method;
    }

    public static void setReplayingRecording(MethodRecording methodRecording) {
        JackboxAspect.methodRecording.set(methodRecording);
        replayMode = true;
    }

    public static void clearReplayingRecording() {
        JackboxAspect.methodRecording.set(null);
        replayMode = false;
    }
}
