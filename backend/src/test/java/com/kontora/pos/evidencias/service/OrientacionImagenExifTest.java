package com.kontora.pos.evidencias.service;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrientacionImagenExifTest {

    private static final int A = 0xFFFF0000;
    private static final int B = 0xFF00FF00;
    private static final int C = 0xFF0000FF;
    private static final int D = 0xFFFFFF00;
    private static final int E = 0xFFFF00FF;
    private static final int F = 0xFF00FFFF;

    @Test
    void aplicaLasOchoOrientacionesExifALosPixeles() {
        BufferedImage original = imagenMuestra();
        Map<Integer, int[][]> esperadas = Map.of(
                1, new int[][]{{A, B, C}, {D, E, F}},
                2, new int[][]{{C, B, A}, {F, E, D}},
                3, new int[][]{{F, E, D}, {C, B, A}},
                4, new int[][]{{D, E, F}, {A, B, C}},
                5, new int[][]{{A, D}, {B, E}, {C, F}},
                6, new int[][]{{D, A}, {E, B}, {F, C}},
                7, new int[][]{{F, C}, {E, B}, {D, A}},
                8, new int[][]{{C, F}, {B, E}, {A, D}});

        esperadas.forEach((orientacion, pixelesEsperados) -> {
            BufferedImage resultado = OrientacionImagenExif.normalizar(
                    original,
                    jpegExif(orientacion, true));

            assertThat(resultado.getHeight())
                    .as("alto para orientacion %s", orientacion)
                    .isEqualTo(pixelesEsperados.length);
            assertThat(resultado.getWidth())
                    .as("ancho para orientacion %s", orientacion)
                    .isEqualTo(pixelesEsperados[0].length);
            for (int y = 0; y < pixelesEsperados.length; y++) {
                for (int x = 0; x < pixelesEsperados[y].length; x++) {
                    assertThat(resultado.getRGB(x, y))
                            .as("pixel %s,%s para orientacion %s", x, y, orientacion)
                            .isEqualTo(pixelesEsperados[y][x]);
                }
            }
        });
    }

    @Test
    void reconoceExifBigEndian() {
        assertThat(OrientacionImagenExif.leerOrientacion(jpegExif(8, false))).isEqualTo(8);
    }

    @Test
    void conservaImagenSinExifOConMetadataInvalida() {
        BufferedImage original = imagenMuestra();

        assertThat(OrientacionImagenExif.normalizar(original, new byte[]{1, 2, 3}))
                .isSameAs(original);
        assertThat(OrientacionImagenExif.leerOrientacion(jpegExif(9, true))).isEqualTo(1);
    }

    private BufferedImage imagenMuestra() {
        BufferedImage imagen = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        int[][] pixeles = {{A, B, C}, {D, E, F}};
        for (int y = 0; y < pixeles.length; y++) {
            for (int x = 0; x < pixeles[y].length; x++) {
                imagen.setRGB(x, y, pixeles[y][x]);
            }
        }
        return imagen;
    }

    private byte[] jpegExif(int orientacion, boolean littleEndian) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0xFF);
        output.write(0xD8);
        output.write(0xFF);
        output.write(0xE1);
        output.write(0x00);
        output.write(0x22);
        output.writeBytes(new byte[]{'E', 'x', 'i', 'f', 0, 0});

        if (littleEndian) {
            output.writeBytes(new byte[]{
                    'I', 'I', 0x2A, 0,
                    8, 0, 0, 0,
                    1, 0,
                    0x12, 0x01,
                    3, 0,
                    1, 0, 0, 0,
                    (byte) orientacion, 0, 0, 0,
                    0, 0, 0, 0
            });
        } else {
            output.writeBytes(new byte[]{
                    'M', 'M', 0, 0x2A,
                    0, 0, 0, 8,
                    0, 1,
                    0x01, 0x12,
                    0, 3,
                    0, 0, 0, 1,
                    0, (byte) orientacion, 0, 0,
                    0, 0, 0, 0
            });
        }

        output.write(0xFF);
        output.write(0xD9);
        return output.toByteArray();
    }
}
