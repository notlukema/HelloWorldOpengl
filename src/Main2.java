import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;

import jaco.mp3.player.MP3Player;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/* Wav file playing imports
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.BufferedInputStream;
import java.util.Objects;
 */

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL.createCapabilities;
import static org.lwjgl.opengl.GL11.*; // Yeah immediate rendering is goofy
import static org.lwjgl.stb.STBTruetype.*;

public class Main2 {

    // High quality spinning hello world with high quality music?
    static final boolean HIGH_QUALITY = false;

    // Texture data required for drawing (static for simplicity of access)
    static int texID;
    static STBTTFontinfo info;
    static STBTTBakedChar.Buffer chars;
    static int bitmapSize, fontSize;
    static float descent;

    // Size of window, saved for later use
    static int width, height;

    public static boolean loadFont(String file, int size) {

        // Loads data into byte array and returns false if unsuccessful
        byte[] data;
        try (java.io.InputStream stream = Main2.class.getResourceAsStream(file)) {
            data = stream.readAllBytes();
        } catch (NullPointerException | IOException e) {
            System.err.println("Couldn't load file: " + file);
            e.printStackTrace();
            return false;
        }

        // Loads byte array into byte buffer
        ByteBuffer font = BufferUtils.createByteBuffer(data.length);
        font.put(data);
        font.flip();

        // Loads font data
        info = STBTTFontinfo.create();
        stbtt_InitFont(info, font);

        // Bitmap size to bake font into
        fontSize = size;
        bitmapSize = fontSize * 8;

        // Loads y data for font, used for centering text
        int[] desc = new int[1];
        stbtt_GetFontVMetrics(info, new int[1], desc, new int[1]);
        descent = desc[0] * stbtt_ScaleForPixelHeight(info, fontSize);

        // Creates bitmap and loads data for 96 characters (all basic letters, not including special characters)
        ByteBuffer bitmap = BufferUtils.createByteBuffer(bitmapSize * bitmapSize);
        chars = STBTTBakedChar.create(96);

        // Bakes font into bitmap
        stbtt_BakeFontBitmap(font, fontSize, bitmap, bitmapSize, bitmapSize, 32, chars);

        // Generates OpenGL texture and loads bitmap data (alpha texture, not rgba)
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, bitmapSize, bitmapSize, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        // Successfully loaded font
        return true;
    }

    public static void drawString(String text, int size) {

        // Find ratio based on new size to baked size
        float ratio = (float)size / fontSize;

        // Array for data on each letter (extra steps required to center text)
        STBTTAlignedQuad[] quads = new STBTTAlignedQuad[text.length()];
        float[] advance = new float[text.length()];
        float wid = 0;

        // Loads data into array while finding width of text for centering
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 31 && c < 128) {
                STBTTAlignedQuad q = STBTTAlignedQuad.create();
                float[] adv = new float[1];
                stbtt_GetBakedQuad(chars, bitmapSize, bitmapSize, c - 32, adv, new float[1], q, true);
                quads[i] = q;
                advance[i] = adv[0];
                wid += adv[0];
            } else {
                quads[i] = null;
                advance[i] = 0;
            }
        }

        // Centers text
        float x = -wid/2;
        float y = descent;

        // Draw letters
        glBegin(GL_QUADS);
        for (int i = 0; i < quads.length; i++) {
            STBTTAlignedQuad q;
            if ((q = quads[i]) == null) {
                continue;
            }

            // Each letter is rendered as a quad mapped to a quad of the letter on the font texture (baked bitmap)

            glTexCoord2f(q.s0(),q.t1());
            glVertex3f((x + q.x0()) * ratio / width,(y + q.y1()) * ratio / height, 0f);

            glTexCoord2f(q.s1(),q.t1());
            glVertex3f((x + q.x1()) * ratio / width,(y + q.y1()) * ratio / height, 0f);

            glTexCoord2f(q.s1(),q.t0());
            glVertex3f((x + q.x1()) * ratio / width,(y - q.y0()) * ratio / height, 0f);

            glTexCoord2f(q.s0(),q.t0());
            glVertex3f((x + q.x0()) * ratio / width,(y - q.y0()) * ratio / height, 0f);

            x += advance[i];
        }
        glEnd();
    }

    public static void main(String[] args) {

        // Initiates GLFW and exits if unsuccessful
        if (!glfwInit()) {
            System.err.println("Couldn't load glfw wat???");
            System.exit(1);
        }

        // Creates window and OpenGL context (handled by GLFW)
        width = 800;
        height = 800;
        long id = glfwCreateWindow(width, height, "spinning hello world", 0, 0);
        glfwMakeContextCurrent(id);
        createCapabilities();

        // Loads font and exits if unsuccessful
        if (!loadFont("Ldfcomicsans-jj7l.ttf", HIGH_QUALITY ? 64 : 16)) {
            System.err.println("\nCouldn't load font??? Probably couldn't find file lol");
            System.exit(1);
        }

        // Loads and plays music
        String musicFile;
        if (HIGH_QUALITY) {
            musicFile = "funky-town-cut.mp3";
        } else {
            musicFile = "funky-town-low-quality.mp3";
        }

        // mp3 files are chosen even if it means including another library since their size is just so much smaller than wav files
        new MP3Player(new File("src\\" + musicFile)).play();

        /* Old code to play .wav files, no extra library needed
        try (AudioInputStream audioInputStream =
                     AudioSystem.getAudioInputStream(
                             new BufferedInputStream(
                                     Objects.requireNonNull(Main2.class.getResourceAsStream(musicFile))))) {
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
        } catch(Exception e) { // Way too many exception types lol
            System.err.println("Couldn't load and play sound: " + musicFile);
            e.printStackTrace();
        }
         */

        // Preparing for text rotation
        float rot = 0;
        long lastTime = System.nanoTime();

        // Runs loop until the window is closed
        while(!glfwWindowShouldClose(id)) {

            // Increments rotation based on time instead of frame rate
            long time = System.nanoTime();
            rot += (time - lastTime) * 0.000000004f;
            lastTime = time;

            // Polls glfw events such as callbacks, inputs, and other critical functions
            glfwPollEvents();

            // Clears color buffer and depth buffer
            glClearColor(0f, 0f, 0f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Enables depth testing for 3d and blending for transparency (alpha value)
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // Loads projection matrix (3d coordinates to 2d coordinates)
            FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16); // Remember, the matrix is 4x4
            glMatrixMode(GL_PROJECTION);
            glLoadMatrixf(new Matrix4f().perspective(70 * (float)Math.PI / 180, (float)width/height, -1f, 1f).get(floatBuffer));

            // Loads a transformation matrix for modifying the object positions
            glMatrixMode(GL_MODELVIEW);
            glLoadMatrixf(new Matrix4f().identity()
                    .rotateX(0.2f)
                    .translate(0, -0.35f, -1.75f)
                    .rotateY(rot)
                    .get(floatBuffer));

            // Have to enable for using textures, remember to disable when not using textures
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, texID);

            // Set color and draw string
            glColor4f(1f, 1f, 1f, 1f);
            drawString("Hello World!", 256);

            /* Code to render baked font bitmap
            glBegin(GL_QUADS);
            glTexCoord2f(0, 1);glVertex2f(-1, -1);
            glTexCoord2f(1, 1);glVertex2f(1, -1);
            glTexCoord2f(1, 0);glVertex2f(1, 1);
            glTexCoord2f(0, 0);glVertex2f(-1, 1);
            glEnd();
             */

            // Disable if drawing non-textured objects
            //glDisable(GL_TEXTURE_2D);

            // Flushes all commands, not required due to lack of intensive operations
            glFlush();

            // Swaps window buffers
            glfwSwapBuffers(id);
        }

        // Just making sure program was successful
        System.out.println("Successful program exit");
        System.exit(0);
    }

}
