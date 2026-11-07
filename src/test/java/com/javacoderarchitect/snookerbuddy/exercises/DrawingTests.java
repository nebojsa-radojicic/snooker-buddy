package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DrawingTests {
    @Test
    void convertsTransparentPngToWhiteBackgroundWithoutUpscaling() throws Exception {
        var image = new BufferedImage(30, 20, BufferedImage.TYPE_INT_ARGB);
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        var drawing = DrawingService.prepare(output.toByteArray());
        var result = ImageIO.read(new ByteArrayInputStream(drawing.bytes()));
        assertThat(drawing.contentType()).isEqualTo("image/jpeg");
        assertThat(result.getWidth()).isEqualTo(30);
        assertThat(result.getHeight()).isEqualTo(20);
        assertThat(new Color(result.getRGB(0, 0))).isEqualTo(Color.WHITE);
    }

    @Test
    void rejectsSvgRatherThanServingActiveContent() {
        assertThatThrownBy(() -> DrawingService.prepare("<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
