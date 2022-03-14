package me.salamander.mallet.compiler;

import me.salamander.mallet.compiler.instruction.value.Variable;
import me.salamander.mallet.compiler.resolution.ClassResolver;
import me.salamander.mallet.compiler.resolution.DefaultResolver;
import me.salamander.mallet.compiler.type.BasicType;
import me.salamander.mallet.compiler.type.EnumType;
import me.salamander.mallet.compiler.type.MalletType;
import me.salamander.mallet.compiler.type.StructType;
import me.salamander.mallet.util.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class GlobalCompilationContext {
    private final TreeSet<ClassResolver> resolvers = new TreeSet<>(Comparator.comparingInt(ClassResolver::getPriority).reversed());
    private final Map<String, ClassNode> cachedClasses = new HashMap<>();
    private final Map<Type, MalletType> types = new HashMap<>();

    public GlobalCompilationContext(){
        addDefaultResolvers();
        addDefaultTypes();
    }

    public void addClassResolver(ClassResolver resolver) {
        resolvers.add(resolver);
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

        for (ClassResolver resolver : resolvers) {
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
        addClassResolver(new DefaultResolver());
    }

    private void addDefaultTypes() {
        types.put(BasicType.INT.getJavaType(), BasicType.INT);
        types.put(BasicType.FLOAT.getJavaType(), BasicType.FLOAT);
        types.put(BasicType.DOUBLE.getJavaType(), BasicType.DOUBLE);
        types.put(BasicType.BOOLEAN.getJavaType(), BasicType.BOOLEAN);

        types.put(BasicType.VEC2.getJavaType(), BasicType.VEC2);
        types.put(BasicType.VEC3.getJavaType(), BasicType.VEC3);
        types.put(BasicType.VEC4.getJavaType(), BasicType.VEC4);

        types.put(BasicType.VEC3I.getJavaType(), BasicType.VEC3I);
    }

    public MalletType getType(Type type) {
        return types.computeIfAbsent(type, this::makeType);
    }

    private MalletType makeType(Type type) {
        Class<?> clazz = Util.getClass(type);

        if (Enum.class.isAssignableFrom(clazz)) {
            return new EnumType(type);
        } else {
            return new StructType(type);
        }
    }
}
