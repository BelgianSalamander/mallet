package me.salamander.mallet;

import me.salamander.mallet.resolution.AnnotationResolver;
import me.salamander.mallet.resolution.DefaultAnnotationResolver;
import me.salamander.mallet.shaders.compiler.instruction.value.Variable;
import me.salamander.mallet.resolution.ClassResolver;
import me.salamander.mallet.resolution.DefaultClassResolver;
import me.salamander.mallet.type.BasicType;
import me.salamander.mallet.type.EnumType;
import me.salamander.mallet.type.MalletType;
import me.salamander.mallet.type.StructType;
import me.salamander.mallet.util.AnnotationList;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class MalletContext {
    private final TreeSet<ClassResolver> classResolvers = new TreeSet<>(Comparator.comparingInt(ClassResolver::getPriority).reversed());
    private final TreeSet<AnnotationResolver> annotationResolvers = new TreeSet<>(Comparator.comparingInt(AnnotationResolver::getPriority).reversed());

    private final Map<String, ClassNode> cachedClasses = new HashMap<>();
    private final Map<Type, MalletType> types = new HashMap<>();

    private final Set<Type> beingCreated = new HashSet<>();

    public MalletContext(){
        addDefaultResolvers();
        addDefaultTypes();
    }

    public void addClassResolver(ClassResolver resolver) {
        classResolvers.add(resolver);
    }

    public ClassNode findClass(Class<?> clazz){
        return findClass(clazz.getName());
    }

    public ClassNode findClass(Type type){
        return findClass(type.getClassName());
    }

    public ClassNode findClass(String name) {
        synchronized (cachedClasses) {
            if (cachedClasses.containsKey(name)) {
                return cachedClasses.get(name);
            }
        }

        for (ClassResolver resolver : classResolvers) {
            ClassNode node = resolver.tryResolve(name);
            if (node != null) {
                synchronized (cachedClasses) {
                    cachedClasses.put(name, node);
                }
                return node;
            }
        }

        throw new RuntimeException("Could not find class " + name);
    }

    public void addAnnotationResolver(AnnotationResolver resolver) {
        annotationResolvers.add(resolver);
    }

    public AnnotationList getAnnotations(Class<?> clazz) {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationResolver annotationResolver : annotationResolvers) {
            annotationResolver.getClassAnnotations(annotations, clazz);
        }
        return new AnnotationList(annotations);
    }

    public AnnotationList getAnnotations(Field field) {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationResolver annotationResolver : annotationResolvers) {
            annotationResolver.getFieldAnnotations(annotations, field);
        }
        return new AnnotationList(annotations);
    }

    public AnnotationList getAnnotations(Method method) {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationResolver annotationResolver : annotationResolvers) {
            annotationResolver.getMethodAnnotations(annotations, method);
        }
        return new AnnotationList(annotations);
    }

    public AnnotationList getParameterAnnotations(Method method, int index) {
        List<Annotation> annotations = new ArrayList<>();
        for (AnnotationResolver annotationResolver : annotationResolvers) {
            annotationResolver.getParameterAnnotations(annotations, method, index);
        }
        return new AnnotationList(annotations);
    }

    public String varName(Variable variable) {
        StringBuilder sb = new StringBuilder();

        sb.append(variable.getVariableType().getPrefix());
        sb.append("var_");
        sb.append(variable.getIndex());
        sb.append("_");
        sb.append(getType(variable.getType()).getName());

        return Util.removeSpecial(sb.toString());
    }

    private void addDefaultResolvers() {
        addClassResolver(new DefaultClassResolver());

        addAnnotationResolver(new DefaultAnnotationResolver());
    }

    private void addDefaultTypes() {
        for (BasicType basicType : BasicType.makeTypes(this)) {
            this.types.put(basicType.getJavaType(), basicType);
        }
    }

    public MalletType getType(Type type) {
        if (types.containsKey(type)) {
            return types.get(type);
        }

        if (beingCreated.contains(type)) {
            throw new RuntimeException("Cyclic type dependency: " + type);
        }

        beingCreated.add(type);

        this.types.put(type, makeType(type));

        beingCreated.remove(type);

        return types.get(type);
    }

    private MalletType makeType(Type type) {
        Class<?> clazz = Util.getClass(type);

        if (Enum.class.isAssignableFrom(clazz)) {
            return EnumType.of((Class<? extends Enum<?>>) clazz, this);
        } else {
            return new StructType(type, this);
        }
    }
}
