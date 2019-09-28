package cpw.mods.forge.cursepacklocator;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_DEBUG_CONTEXT;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.stb.STBEasyFont;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

class ProgressOutput {

    //private final Dist dist;
    private int totalFiles;
    private final AtomicInteger downloadedCount = new AtomicInteger(0);
    private volatile boolean isRunning = false;

    private final int screenWidth = 550;
    private final int screenHeight = 60;

    ProgressOutput(/*Dist dist*/) {
        // this.dist = dist; // I can't figure out how to access dist here
    }

    void progressDisplayLoop() {
        //if (!dist.isClient()) {
        //    return;
        //}
        try {
            glfwDrawLoop();
        } catch (Throwable t) {
            // ignore, this probably means we are dedicated server. TODO: This needs to be changed
        }
    }

    private void glfwDrawLoop() {
        isRunning = true;
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        long window = glfwCreateWindow(screenWidth, screenHeight, "CursePackLocator mod download progress", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window"); // ignore it and make the GUI optional?
        }

        glfwSetKeyCallback(window, (windowHandle, key, scancode, action, mods) -> {
            // handle quitting here?
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();
        glClearColor(0.1f, 0.1f, 0.3f, 0.0f);

        while (isRunning && !glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            drawProgress(downloadedCount.get() / (float) totalFiles);
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();

    }

    private void drawProgress(final float progress) {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0.0D, screenWidth, screenHeight, 0.0D, -1000.0D, 1000.0D);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // replace with more modern opengl?
        glBegin(GL_QUADS);
        glColor3f(0.1f, 0.1f, 0.9f);
        glVertex2f(0, 0);
        glVertex2f(0, screenHeight);
        glVertex2f(screenWidth * progress, screenHeight);
        glVertex2f(screenWidth * progress, 0);
        glEnd();

        glEnableClientState(GL11.GL_VERTEX_ARRAY);
        glEnable(GL_BLEND);

        String message = String.format("Downloading curse mod pack: %.1f%%", progress * 100);
        ByteBuffer charBuffer = MemoryUtil.memAlloc(message.length() * 270);
        int quads = STBEasyFont.stb_easy_font_print(0, 0, message, null, charBuffer);

        glVertexPointer(3, GL11.GL_FLOAT, 16, charBuffer);
        glPushMatrix();
        glTranslatef(1, screenHeight / 2f - STBEasyFont.stb_easy_font_height(message) / 2f, 0);
        glScalef(2, 2, 1);
        glColor3f(0, 0, 0);
        glDrawArrays(GL11.GL_QUADS, 0, quads * 4);
        glPopMatrix();
        MemoryUtil.memFree(charBuffer);
        //System.exit(-1);
    }

    void finish() {
        isRunning = false;
    }

    void setFileCount(final int size) {
        totalFiles = size;
    }

    // these 2 methods can be used to display the mods currently being downloaded. It may not be very useful without a file name.

    void beginFile(final String projectID, final String fileID, final String fileName) {
    }

    void endFile(final String projectID, final String fileID) {
        downloadedCount.incrementAndGet();
    }
}
