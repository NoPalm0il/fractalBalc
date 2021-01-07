package utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageUtils {

    public static int[][] imageToColorArray(BufferedImage image) {
        int[][] buffer = new int[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                buffer[y][x] = image.getRGB(x, y);
            }
        }
        return buffer;
    }

    public static BufferedImage colorArrayToImage(int[][] buffer) {
        BufferedImage image = new BufferedImage(buffer[0].length, buffer.length, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, buffer[y][x]);
            }
        }
        return image;
    }

    public static byte[] imageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        baos.flush();
        return baos.toByteArray();
    }

    public static BufferedImage byteArrayToImage(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    /**
     * @param x      - starting x coord
     * @param y      - starting y coord
     * @param buffer - color buffer to paint the image
     */
    public static void paintBufferedImage(int x, int y, int[][] buffer, BufferedImage image) {
        for (int yy = y; yy < buffer.length; yy++) {
            for (int xx = x; xx < buffer[0].length; xx++) {
                image.setRGB(xx, yy, buffer[yy - y][xx - x]);
            }
        }
    }
}
