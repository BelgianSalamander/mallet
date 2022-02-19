package me.salamander.mallet.compiler;

import me.salamander.mallet.compiler.resolution.ClassResolver;
import me.salamander.mallet.compiler.resolution.DefaultResolver;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class GlobalCompilationContext {
    private final TreeSet<ClassResolver> resolvers = new TreeSet<>(Comparator.comparingInt(ClassResolver::getPriority).reversed());
    private final Map<String, ClassNode> cachedClasses = new HashMap<>();

    public GlobalCompilationContext(){
        addDefaultResolvers();
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

    private void addDefaultResolvers() {
        addClassResolver(new DefaultResolver());
    }
}
