package me.salamander.mallet.window;

import me.salamander.mallet.Mallet;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.*;

public class Window {
    private final long windowHandle;
    private boolean destroyed = false;

    private Monitor monitor;

    private int width, height;

    //Stores the window position and size before it was full-screened so it can be retrieved
    private int previousX = -1, previousY;
    private int previousHeight, previousWidth;

    private final Set<GLFWCharCallbackI> charCallbacks = new HashSet<>();
    private final Set<GLFWKeyCallbackI> keyCallbacks = new HashSet<>();
    private final Set<GLFWScrollCallbackI> scrollCallbacks = new HashSet<>();
    private final Set<GLFWCursorPosCallbackI> cursorPosCallbacks = new HashSet<>();
    private final Set<GLFWMouseButtonCallbackI> mouseButtonCallbacks = new HashSet<>();

    public Window(WindowOptions options) {
        Mallet.init();

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, Mallet.MAJOR_VERSION);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, Mallet.MINOR_VERSION);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_REFRESH_RATE, 1);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_SAMPLES, options.getSamples());

        long monitor = options.isFullscreen() ? glfwGetPrimaryMonitor() : 0;

        if(options.isFullscreen()){
            this.monitor = new Monitor(monitor);
        }

        this.windowHandle = glfwCreateWindow(options.getWidth(), options.getHeight(), options.getTitle(), monitor, 0);

        makeCurrent();
        GL.createCapabilities();

        glfwSetWindowCloseCallback(windowHandle, this::onClose);
        glfwSetFramebufferSizeCallback(windowHandle, this::onResize);

        glfwSetCharCallback(windowHandle, this::onChar);
        glfwSetKeyCallback(windowHandle, this::onKey);
        glfwSetScrollCallback(windowHandle, this::onScroll);
        glfwSetCursorPosCallback(windowHandle, this::onCursorPos);
        glfwSetMouseButtonCallback(windowHandle, this::onMouseButton);

        this.width = options.getWidth();
        this.height = options.getHeight();

        if(options.getX() != -1 && options.getY() != -1){
            setPos(options.getX(), options.getY());
        }

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        //Make wireframe
        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        //Enable scissor
        glEnable(GL_SCISSOR_TEST);

        if (options.getSamples() != 1) {
            glEnable(GL_MULTISAMPLE);
        }
    }

    public static void shutdownSubsystem() {
        glfwTerminate();
    }

    private void onMouseButton(long window, int button, int action, int mods) {
        this.mouseButtonCallbacks.forEach(c -> c.invoke(window, button, action, mods));
    }

    private void onCursorPos(long window, double x, double y) {
        this.cursorPosCallbacks.forEach(c -> c.invoke(window, x, y));
    }

    private void onScroll(long window, double x, double y) {
        this.scrollCallbacks.forEach(c -> c.invoke(window, x, y));
    }

    private void onKey(long window, int key, int scancode, int action, int mods) {
        this.keyCallbacks.forEach(c -> c.invoke(window, key, scancode, action, mods));
    }

    private void onChar(long window, int codepoint) {
        this.charCallbacks.forEach(c -> c.invoke(window, codepoint));
    }

    /**
     * Checks whether or not the SHOULD_CLOSE flag is set for this window
     * @return If this window should close
     */
    public boolean shouldClose(){
        return glfwWindowShouldClose(windowHandle);
    }

    /**
     * Destroys the window and context. Any subsequent operations on this object will result in unpredicted behaviour
     */
    public void destroy(){
        if(destroyed) return;

        glfwDestroyWindow(windowHandle);

        destroyed = true;
    }

    /**
     * Makes this window the current context for the thread
     */
    public void makeCurrent(){
        glfwMakeContextCurrent(windowHandle);
    }

    /**
     * Clears the color and depth buffers of this window. The window must be the current context
     */
    public void clear(){
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Swaps the framebuffers of this window.
     */
    public void endFrame(){
        glfwSwapBuffers(windowHandle);
    }

    /**
     * Makes this window visible
     */
    public void show(){
        glfwShowWindow(windowHandle);
    }

    /**
     * Hides this window
     */
    public void hide(){
        glfwHideWindow(windowHandle);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDestroyed(){
        return destroyed;
    }

    /**
     * Sets the clear (background) color of this window
     * @param color The color to use
     */
    public void setClearColor(Color color){
        setClearColor(color.getRed() / 255.f, color.getGreen() / 255.f, color.getBlue() / 255.f, color.getAlpha() / 255.f);
    }

    /**
     * Sets the clear (background) color of this window
     * @param rgb The color to use packed into an int
     */
    public void setClearColor(int rgb){
        setClearColor(((rgb >> 16) & 0xff) / 255.f, ((rgb >> 8) & 0xff) / 255.f, (rgb & 0xff) / 255.f, ((rgb >> 24) & 0xff) / 255.f);
    }

    /**
     * Sets the clear (background) color of this window
     * @param r The red value of the color in the closed interval [0, 255]
     * @param g The green value of the color in the closed interval [0, 255]
     * @param b The blue value of the color in the closed interval [0, 255]
     * @param a The alpha value of the color in the closed interval [0, 255]
     */
    public void setClearColor(int r, int g, int b, int a){
        setClearColor(r / 255.f, g / 255.f, b / 255.f, a / 255.f);
    }

    /**
     * Sets the clear (background) color of this winodw
     * @param r The red value of the color in the closed interval [0 ,1]
     * @param g The green value of the color in the closed interval [0 ,1]
     * @param b The blue value of the color in the closed interval [0 ,1]
     * @param a The alpha value of the color in the closed interval [0 ,1]
     */
    public void setClearColor(float r, float g, float b, float a) {
        makeCurrent();
        glClearColor(r, g, b, a);
    }

    /**
     * Manually set the dimensions of the window
     * @param width The new width in pixels
     * @param height The new height in pixels
     */
    public void setSize(int width, int height){
        glfwSetWindowSize(windowHandle, width, height);
    }

    /**
     * Manually set the position of the window
     */
    public void setPos(int x, int y){
        glfwSetWindowPos(windowHandle, x, y);
    }

    /**
     * Minifies (iconifies) the window
     */
    public void minimize(){
        glfwIconifyWindow(windowHandle);
    }

    /**
     * Maximizes the window
     */
    public void maximize(){glfwMaximizeWindow(windowHandle);}

    /**
     * Restore the window. If it is minimized it gets opened and if it is maximized it gets restored to normal
     */
    public void restore(){
        glfwRestoreWindow(windowHandle);
    }

    public boolean isMinimized(){
        return glfwGetWindowAttrib(windowHandle, GLFW_ICONIFIED) == 1;
    }

    /**
     * Sets the focus on this window
     */
    public void focus(){
        glfwFocusWindow(windowHandle);
    }

    public boolean isFocused(){
        return glfwGetWindowAttrib(windowHandle, GLFW_FOCUSED) == 1;
    }

    /**
     * Request user's attention without interrupting. (In windows this makes the icon orange)
     */
    public void requestAttention(){
        glfwRequestWindowAttention(windowHandle);
    }

    /**
     * @return The window's content scale on the X and Y axis
     */
    public ContentScale getContentScale(){
        float[] x = new float[1];
        float[] y = new float[1];

        glfwGetWindowContentScale(windowHandle, x, y);

        return new ContentScale(x[0], y[0]);
    }

    public void setFullscreen(){
        setFullscreen(Monitor.getPrimaryMonitor());
    }

    public void setFullscreen(Monitor monitor){
        GLFWVidMode videoMode = monitor.getCurrentVideoMode();

        previousWidth = width;
        previousHeight = height;

        int[] xTemp = new int[1];
        int[] yTemp = new int[1];
        glfwGetWindowPos(windowHandle, xTemp, yTemp);
        previousX = xTemp[0];
        previousY = yTemp[0];

        glfwSetWindowMonitor(windowHandle, monitor.handle, 0, 0, videoMode.width(), videoMode.height(), videoMode.refreshRate());
    }

    public void exitFullscreen(){
        if(glfwGetWindowMonitor(windowHandle) != 0){
            int width, height, x, y;
            if(previousX == -1){
                width = 640;
                height = 480;

                x = 100;
                y = 100;
            }else{
                width = previousWidth;
                height = previousHeight;

                x = previousX;
                y = previousY;
            }

            glfwSetWindowMonitor(windowHandle, 0, x, y, width, height, 0);
            glfwSwapInterval(1);
        }
    }

    public void setTitle(String name){
        glfwSetWindowTitle(windowHandle, name);
    }

    /**
     * Returns an orthographic projection matrix that results in all the points who's x is in the range [-(aspectRatio * size), aspectRatio * size],
     * who's y is in the range [-size, size] and who's z is in the range [0, -depth]
     * @return
     */
    public Matrix4f getOrthographicProjectionMatrix(float size, float depth){
        return new Matrix4f().scale(1 / ((float) width / height * size), 1 / size, 1 / depth);
    }

    private void onClose(long windowHandle) {

    }

    private void onResize(long windowHandle, int width, int height) {
        glViewport(0, 0, width, height);

        this.width = width;
        this.height = height;
    }

    public void addCharCallback(GLFWCharCallbackI callback){
        this.charCallbacks.add(callback);
    }

    public void addKeyCallback(GLFWKeyCallbackI callback){
        this.keyCallbacks.add(callback);
    }

    public void addMouseButtonCallback(GLFWMouseButtonCallbackI callback){
        this.mouseButtonCallbacks.add(callback);
    }

    public void addScrollCallback(GLFWScrollCallbackI callback){
        this.scrollCallbacks.add(callback);
    }

    public void addCursorPosCallback(GLFWCursorPosCallbackI callback){
        this.cursorPosCallbacks.add(callback);
    }

    public void hideCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }

    public void setCursorPos(float prevX, float prevY) {
        glfwSetCursorPos(windowHandle, prevX, prevY);
    }

    public void showCursor() {
        glfwSetInputMode(windowHandle, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    public void beginFrame() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glScissor(- (1 << 13), - (1 << 13), (1 << 13) * 2, (1 << 13) * 2);
        GLFW.glfwPollEvents();
    }

    public long getHandle() {
        return windowHandle;
    }

    public String[] getSupportedExtensions(){
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer numExtensions = stack.mallocInt(1);
            glGetIntegerv(GL_NUM_EXTENSIONS, numExtensions);

            String[] extensions = new String[numExtensions.get(0)];

            for (int i = 0; i < extensions.length; i++) {
                extensions[i] = glGetStringi(GL_EXTENSIONS, i);
            }

            return extensions;
        }
    }

    static public record ContentScale(float xScale, float yScale){

    }
}
