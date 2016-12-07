package net.lr.brave.closure;

import com.github.kristofa.brave.Brave;

public class FunctionalTracer {
    private Brave brave;
    private String component;

    public FunctionalTracer(Brave brave, String component) {
        this.brave = brave;
        this.component = component;
    }
    
    public void trace(String operation, Runnable run) {
        brave.localTracer().startNewSpan(component, operation);
        try {
            run.run();
        } catch (RuntimeException e) {
            annotateException(e);
            throw e;
        } finally {
            brave.localTracer().finishSpan();
        }
    }
    
    public <T> T trace(String operation, MyCallable<T> run) {
        brave.localTracer().startNewSpan(component, operation);
        try {
            return run.call();
        } catch (Exception e) {
            annotateException(e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else {
                throw new RuntimeException(e.getMessage(), e);
            }
        } finally {
            brave.localTracer().finishSpan();
        }
    }

    private void annotateException(Exception e) {
        brave.localTracer().submitBinaryAnnotation("exceptionClass", e.getClass().getName());
        brave.localTracer().submitBinaryAnnotation("exceptionMessage", e.getMessage());
    }
}
