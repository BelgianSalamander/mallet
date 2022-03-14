package me.salamander.mallet;

import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.glfw.GLFW.*;

public class Mallet {
    public static final int MAJOR_VERSION = 4;
    public static final int MINOR_VERSION = 5;
    public static final String GL_VERSION = String.valueOf(MAJOR_VERSION) + MINOR_VERSION + "0";

    private static final ThreadLocal<Boolean> glInitialized = ThreadLocal.withInitial(() -> false);

    public static void init() {
        if (!glInitialized.get()) {
            if(!glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }
            glInitialized.set(true);
        }
    }
}
