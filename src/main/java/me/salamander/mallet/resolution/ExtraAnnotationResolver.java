package me.salamander.mallet.resolution;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtraAnnotationResolver implements AnnotationResolver {
    private static final Map<Class<?>, List<Annotation>> extraAnnotations = new HashMap<>();

    public ExtraAnnotationResolver add(Class<?> clazz, Annotation annotation) {
        extraAnnotations.computeIfAbsent(clazz, k -> new ArrayList<>()).add(annotation);
        return this;
    }

    @Override
    public void getClassAnnotations(List<Annotation> list, Class<?> cls) {
        list.addAll(extraAnnotations.getOrDefault(cls, List.of()));
    }

    @Override
    public void getFieldAnnotations(List<Annotation> list, Field field) {

    }

    @Override
    public void getMethodAnnotations(List<Annotation> list, Method method) {

    }

    @Override
    public void getParameterAnnotations(List<Annotation> list, Method method, int index) {

    }

    @Override
    public int getPriority() {
        return 20;
    }
}
