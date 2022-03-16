package me.salamander.mallet.type.construct;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.InsnList;

public interface ObjectConstructor {
    String[] fieldsUsed();
    void construct(MethodVisitor mv, int baseVarIndex);
}
