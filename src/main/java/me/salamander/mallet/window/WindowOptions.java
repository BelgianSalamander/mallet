package me.salamander.mallet.window;

public class WindowOptions {
    private static final String DEFAULT_NAME = "Mallet Window";

    private final int width, height;
    private final String title;
    private final boolean fullscreen;
    private final int samples;

    private final int x, y;

    public WindowOptions(int width, int height, String title, boolean fullscreen, int x, int y, int samples) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.fullscreen = fullscreen;
        this.x = x;
        this.y = y;
        this.samples = samples;
    }

    public WindowOptions(int width, int height){
        this(width, height, DEFAULT_NAME, false, -1, -1, 1);
    }

    public WindowOptions(int width, int height, int samples){
        this(width, height, DEFAULT_NAME, false, -1, -1, samples);
    }

    public WindowOptions(int width, int height, boolean fullscreen){
        this(width, height, DEFAULT_NAME, fullscreen, -1, -1, 1);
    }

    public WindowOptions(int width, int height, String title){
        this(width, height, title, false, -1, -1, 1);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSamples() {
        return samples;
    }
}
