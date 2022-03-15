package me.salamander.mallet.type.io;

import java.lang.reflect.Field;

public interface StructVisitor {
    void enterField(Field field);
    void exitField(Field field);

    void visitField(Field field);
}
