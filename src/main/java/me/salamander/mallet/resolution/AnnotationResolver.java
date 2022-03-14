package me.salamander.mallet.resolution;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public interface AnnotationResolver {
    void getClassAnnotations(List<Annotation> list, Class<?> cls);
    void getFieldAnnotations(List<Annotation> list, Field field);
    void getMethodAnnotations(List<Annotation> list, Method method);
    void getParameterAnnotations(List<Annotation> list, Method method, int index);

    int getPriority();
}
