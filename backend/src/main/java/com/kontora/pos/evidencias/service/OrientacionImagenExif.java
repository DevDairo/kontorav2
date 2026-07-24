package com.kontora.pos.evidencias.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Aplica físicamente la orientación EXIF de un JPEG a sus píxeles.
 *
 * <p>Las cámaras móviles pueden guardar una matriz horizontal y declarar la
 * orientación visual mediante la etiqueta TIFF/EXIF 0x0112. ImageIO decodifica
 * la matriz, pero no garantiza que aplique esa etiqueta. Esta clase la lee
 * antes de que la evidencia sea recodificada y pierda sus metadatos.</p>
 */
final class OrientacionImagenExif {

    private static final int MARCADOR_APP1 = 0xE1;
    private static final int MARCADOR_SOS = 0xDA;
    private static final int MARCADOR_EOI = 0xD9;
    private static final int ETIQUETA_ORIENTACION = 0x0112;
    private static final int TIPO_SHORT = 3;

    private OrientacionImagenExif() {
    }

    static BufferedImage normalizar(BufferedImage imagen, byte[] contenidoOriginal) {
        int orientacion = leerOrientacion(contenidoOriginal);
        if (orientacion == 1) {
            return imagen;
        }

        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        boolean intercambiaDimensiones = orientacion >= 5;
        int anchoDestino = intercambiaDimensiones ? alto : ancho;
        int altoDestino = intercambiaDimensiones ? ancho : alto;

        BufferedImage normalizada = new BufferedImage(
                anchoDestino,
                altoDestino,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalizada.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(imagen, transformacion(orientacion, ancho, alto), null);
        } finally {
            graphics.dispose();
        }
        return normalizada;
    }

    static int leerOrientacion(byte[] contenido) {
        if (contenido == null
                || contenido.length < 4
                || byteSinSigno(contenido[0]) != 0xFF
                || byteSinSigno(contenido[1]) != 0xD8) {
            return 1;
        }

        int posicion = 2;
        while (posicion + 1 < contenido.length) {
            if (byteSinSigno(contenido[posicion]) != 0xFF) {
                break;
            }

            while (posicion < contenido.length && byteSinSigno(contenido[posicion]) == 0xFF) {
                posicion++;
            }
            if (posicion >= contenido.length) {
                break;
            }

            int marcador = byteSinSigno(contenido[posicion++]);
            if (marcador == MARCADOR_SOS || marcador == MARCADOR_EOI) {
                break;
            }
            if (esMarcadorSinLongitud(marcador)) {
                continue;
            }
            if (posicion + 2 > contenido.length) {
                break;
            }

            int longitudSegmento = entero16BigEndian(contenido, posicion);
            posicion += 2;
            int longitudDatos = longitudSegmento - 2;
            if (longitudSegmento < 2 || longitudDatos > contenido.length - posicion) {
                break;
            }

            if (marcador == MARCADOR_APP1) {
                int orientacion = leerOrientacionExif(contenido, posicion, longitudDatos);
                if (orientacion >= 1 && orientacion <= 8) {
                    return orientacion;
                }
            }
            posicion += longitudDatos;
        }
        return 1;
    }

    private static int leerOrientacionExif(byte[] contenido, int inicio, int longitud) {
        if (longitud < 14
                || contenido[inicio] != 'E'
                || contenido[inicio + 1] != 'x'
                || contenido[inicio + 2] != 'i'
                || contenido[inicio + 3] != 'f'
                || contenido[inicio + 4] != 0
                || contenido[inicio + 5] != 0) {
            return 0;
        }

        int inicioTiff = inicio + 6;
        int finSegmento = inicio + longitud;
        if (inicioTiff + 8 > finSegmento) {
            return 0;
        }

        boolean littleEndian;
        if (contenido[inicioTiff] == 'I' && contenido[inicioTiff + 1] == 'I') {
            littleEndian = true;
        } else if (contenido[inicioTiff] == 'M' && contenido[inicioTiff + 1] == 'M') {
            littleEndian = false;
        } else {
            return 0;
        }

        if (entero16(contenido, inicioTiff + 2, littleEndian) != 42) {
            return 0;
        }

        long desplazamientoIfd = entero32(contenido, inicioTiff + 4, littleEndian);
        if (desplazamientoIfd < 8 || desplazamientoIfd > Integer.MAX_VALUE) {
            return 0;
        }

        int inicioIfd = inicioTiff + (int) desplazamientoIfd;
        if (inicioIfd < inicioTiff || inicioIfd + 2 > finSegmento) {
            return 0;
        }

        int cantidadEntradas = entero16(contenido, inicioIfd, littleEndian);
        int inicioEntradas = inicioIfd + 2;
        for (int indice = 0; indice < cantidadEntradas; indice++) {
            long posicionEntradaLong = (long) inicioEntradas + (long) indice * 12L;
            if (posicionEntradaLong < inicioEntradas || posicionEntradaLong + 12L > finSegmento) {
                return 0;
            }

            int posicionEntrada = (int) posicionEntradaLong;
            int etiqueta = entero16(contenido, posicionEntrada, littleEndian);
            if (etiqueta != ETIQUETA_ORIENTACION) {
                continue;
            }

            int tipo = entero16(contenido, posicionEntrada + 2, littleEndian);
            long cantidad = entero32(contenido, posicionEntrada + 4, littleEndian);
            if (tipo != TIPO_SHORT || cantidad < 1) {
                return 0;
            }

            int orientacion = entero16(contenido, posicionEntrada + 8, littleEndian);
            return orientacion >= 1 && orientacion <= 8 ? orientacion : 0;
        }
        return 0;
    }

    private static AffineTransform transformacion(int orientacion, int ancho, int alto) {
        return switch (orientacion) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, ancho, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, ancho, alto);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, alto);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, alto, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, alto, ancho);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, ancho);
            default -> new AffineTransform();
        };
    }

    private static boolean esMarcadorSinLongitud(int marcador) {
        return marcador == 0x01 || marcador == 0xD8 || marcador >= 0xD0 && marcador <= 0xD7;
    }

    private static int entero16BigEndian(byte[] contenido, int posicion) {
        return byteSinSigno(contenido[posicion]) << 8
                | byteSinSigno(contenido[posicion + 1]);
    }

    private static int entero16(byte[] contenido, int posicion, boolean littleEndian) {
        int primero = byteSinSigno(contenido[posicion]);
        int segundo = byteSinSigno(contenido[posicion + 1]);
        return littleEndian ? primero | segundo << 8 : primero << 8 | segundo;
    }

    private static long entero32(byte[] contenido, int posicion, boolean littleEndian) {
        long primero = byteSinSigno(contenido[posicion]);
        long segundo = byteSinSigno(contenido[posicion + 1]);
        long tercero = byteSinSigno(contenido[posicion + 2]);
        long cuarto = byteSinSigno(contenido[posicion + 3]);
        return littleEndian
                ? primero | segundo << 8 | tercero << 16 | cuarto << 24
                : primero << 24 | segundo << 16 | tercero << 8 | cuarto;
    }

    private static int byteSinSigno(byte valor) {
        return valor & 0xFF;
    }
}
