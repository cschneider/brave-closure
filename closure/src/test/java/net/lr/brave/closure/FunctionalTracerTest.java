package net.lr.brave.closure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.github.kristofa.brave.Brave;

import zipkin.BinaryAnnotation;
import zipkin.BinaryAnnotation.Type;
import zipkin.Span;
import zipkin.reporter.Reporter;

public class FunctionalTracerTest {
    List<Span> spans = new ArrayList<Span>();

    @Test
    public void testRunable() {
        FunctionalTracer tracer = tracer();

        tracer.trace("myop", () -> sleep());

        Assert.assertEquals(1, spans.size());
        Span span = spans.iterator().next();
        Assert.assertEquals("myop", span.name);
        Assert.assertEquals("test", span.serviceNames().iterator().next());
    }
    
    @Test
    public void testCallable() {
        FunctionalTracer tracer = tracer();

        String result = tracer.trace("myFunc", () -> {
            sleep();
            return "myresult";
        });

        Assert.assertEquals("myresult", result);
        Assert.assertEquals(1, spans.size());
        Span span = spans.iterator().next();
        Assert.assertEquals("myfunc", span.name);
        Assert.assertEquals("test", span.serviceNames().iterator().next());
    }

    @Test
    public void testCallableRuntimeException() {
        FunctionalTracer tracer = tracer();

        try {
            tracer.trace("myop", () -> {
                throw new RuntimeException("Something bad happened");
            });
            Assert.fail();
        } catch (RuntimeException e) {
            Assert.assertEquals(1, spans.size());
            Span span = spans.iterator().next();
            Assert.assertEquals("myop", span.name);
            Assert.assertEquals("test", span.serviceNames().iterator().next());
            Assert.assertEquals(3, span.binaryAnnotations.size());
            assertAnnotation(span, "exceptionMessage", "Something bad happened");
            assertAnnotation(span, "exceptionClass", RuntimeException.class.getName());
            assertAnnotation(span, "lc", "mycomp");
        }
    }
    
    private void assertAnnotation(Span span, String key, String value) {
        for (BinaryAnnotation a : span.binaryAnnotations) {
            if (key.equals(a.key) && value.equals(stringValue(a))) {
                return;
            }
        }
        Iterator<BinaryAnnotation> annotations = span.binaryAnnotations.iterator();
        while (annotations.hasNext()) {
            BinaryAnnotation a = annotations.next();
            System.out.println(a.key + ":" + stringValue(a));
        }
        Assert.fail("Annotation " + key + " with value " + value + " not found");
    }

    private String stringValue(BinaryAnnotation annotation) {
        Assert.assertEquals(Type.STRING, annotation.type);
        return new String(annotation.value);
    }


    private FunctionalTracer tracer() {
        Brave brave = new Brave.Builder("test").reporter(simpleReporter()).build();
        FunctionalTracer tracer = new FunctionalTracer(brave, "mycomp");
        return tracer;
    }

    private Reporter<Span> simpleReporter() {
        Reporter<Span> reporter = new Reporter<Span>() {

            @Override
            public void report(Span span) {
                spans.add(span);
            }
        };
        return reporter;
    }

    private void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
