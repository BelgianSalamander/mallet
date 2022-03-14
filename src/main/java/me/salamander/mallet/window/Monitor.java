package me.salamander.mallet.window;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.Objects;

import static org.lwjgl.glfw.GLFW.*;

public class Monitor {
    final long handle;
    private final String name;

    private final boolean isPrimary;

    private final GLFWVidMode[] videoModes;

    public Monitor(long handle){
        this.handle = handle;

        this.name = glfwGetMonitorName(handle);
        this.isPrimary = handle == glfwGetPrimaryMonitor();

        GLFWVidMode.Buffer vidModeBuffer = glfwGetVideoModes(handle);
        videoModes = new GLFWVidMode[vidModeBuffer.remaining()];
        for(int i = 0; i < vidModeBuffer.remaining(); i++){
            videoModes[i] = vidModeBuffer.get();
        }
    }

    /**
     * Get the name of the monitor. This is not guaranteed to be unique
     * @return A human-readable name of this monitor
     */
    public String getName() {
        return name;
    }

    /**
     * Tells you if this is the primary monitor
     * @return Whether or not this monitor is the primary monitor. Only one physical monitor will be the primary however two monitor objects may reference the
     * same physical monitor
     */
    public boolean isPrimary() {
        return isPrimary;
    }

    public GLFWVidMode getCurrentVideoMode(){
        return glfwGetVideoMode(handle);
    }

    @Override
    public String toString() {
        return "Monitor[name = '" + name + "' isPrimary = " + isPrimary + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Monitor monitor = (Monitor) o;
        return handle == monitor.handle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(handle);
    }

    public static Monitor getPrimaryMonitor(){
        return new Monitor(glfwGetPrimaryMonitor());
    }

    /**
     * @return All available physical monitors
     */
    static public Monitor[] getAvailableMonitors(){
        PointerBuffer monitorBuffer = glfwGetMonitors();
        int amount = monitorBuffer.remaining();

        Monitor[] monitors = new Monitor[amount];

        for(int i = 0; i < amount; i++){
            monitors[i] = new Monitor(monitorBuffer.get());
        }

        return monitors;
    }
}
