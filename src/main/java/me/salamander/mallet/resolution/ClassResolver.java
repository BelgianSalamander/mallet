package me.salamander.mallet.resolution;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

public interface ClassResolver {
    /**
     * Tries to find a class with the given name.
     * @param name The name of the class in binary form. (i.e. java.lang.Object)
     * @return The class node, or null if the class could not be found.
     */
    @Nullable
    ClassNode tryResolve(String name);

    /**
     * Higher priority means that this resolver will be used first.
     * @return The priority of this resolver.
     */
    int getPriority();
}
