package me.salamander.mallet.util;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationList {
    private final Map<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();

    public AnnotationList(final List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if(this.annotations.put(annotation.annotationType(), annotation) != null) {
                throw new IllegalArgumentException("AnnotationList already contains annotation of type " + annotation.annotationType());
            }
        }
    }

    public boolean hasAnnotation(final Class<? extends Annotation> annotationClass) {
        return this.annotations.containsKey(annotationClass);
    }

    public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
        return (T) this.annotations.get(annotationClass);
    }
}
