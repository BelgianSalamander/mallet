package me.salamander.mallet.resolution;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;

public class DefaultClassResolver implements ClassResolver {
    public static final int PRIORITY = 10;

    @Override
    public @Nullable ClassNode tryResolve(String name) {
        try {
            ClassNode node = new ClassNode();
            InputStream is = ClassLoader.getSystemResourceAsStream(name.replace('.', '/') + ".class");
            if (is == null) {
                return null;
            }
            ClassReader reader = new ClassReader(is);
            reader.accept(node, 0);
            is.close();
            return node;
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
