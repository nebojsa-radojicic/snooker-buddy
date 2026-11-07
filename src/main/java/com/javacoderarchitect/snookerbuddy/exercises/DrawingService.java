package com.javacoderarchitect.snookerbuddy.exercises;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

final class DrawingService {
    static final long MAX_BYTES = 20 * 1024 * 1024;

    private DrawingService() {}

    static Drawing prepare(byte[] bytes) throws IOException {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Choose a readable JPG or PNG drawing.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("JPEG") && !format.equalsIgnoreCase("PNG"))
                    throw new IllegalArgumentException("Choose a JPG or PNG drawing.");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if ((long) width * height > 60_000_000)
                    throw new IllegalArgumentException("Photo is too large. Choose an image under 60 megapixels.");
                int sample = Math.max(1, Math.max(width, height) / 1600);
                var params = reader.getDefaultReadParam();
                params.setSourceSubsampling(sample, sample, 0, 0);
                var source = reader.read(0, params);
                double scale = Math.min(1, 1600.0 / Math.max(source.getWidth(), source.getHeight()));
                var resized = new BufferedImage(
                        Math.max(1, (int) (source.getWidth() * scale)),
                        Math.max(1, (int) (source.getHeight() * scale)),
                        BufferedImage.TYPE_INT_RGB);
                var graphics = resized.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, resized.getWidth(), resized.getHeight());
                    graphics.setRenderingHint(
                            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.drawImage(source, 0, 0, resized.getWidth(), resized.getHeight(), null);
                } finally {
                    graphics.dispose();
                }
                var output = new ByteArrayOutputStream();
                ImageIO.write(resized, "jpeg", output);
                return new Drawing(output.toByteArray(), "image/jpeg");
            } finally {
                reader.dispose();
            }
        }
    }

    record Drawing(byte[] bytes, String contentType) {}
}
