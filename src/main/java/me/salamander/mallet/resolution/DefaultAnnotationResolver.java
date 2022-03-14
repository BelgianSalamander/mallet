package me.salamander.mallet.resolution;

import me.salamander.mallet.shaders.annotation.NullableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class DefaultAnnotationResolver implements AnnotationResolver {
    public static final int PRIORITY = 10;


    @Override
    public void getClassAnnotations(List<Annotation> list, Class<?> cls) {
        list.addAll(Arrays.asList(cls.getAnnotations()));
    }

    @Override
    public void getFieldAnnotations(List<Annotation> list, Field field) {
        list.addAll(Arrays.asList(field.getAnnotations()));
    }

    @Override
    public void getMethodAnnotations(List<Annotation> list, Method method) {
        list.addAll(Arrays.asList(method.getAnnotations()));
    }

    @Override
    public void getParameterAnnotations(List<Annotation> list, Method method, int index) {
        list.addAll(Arrays.asList(method.getParameterAnnotations()[index]));
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
