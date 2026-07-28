const BYTES_POR_MIB = 1024 * 1024;
const LIMITE_ARCHIVO_TRANSMITIDO = 20 * BYTES_POR_MIB;
const LIMITE_FOTO_HEIC_ORIGINAL = 30 * BYTES_POR_MIB;
const LADO_MAXIMO_FOTO = 3000;
const CALIDAD_JPEG = 0.82;

const MIME_HEIC = new Set([
  "image/heic",
  "image/heic-sequence",
  "image/heif",
  "image/heif-sequence",
]);

const EXTENSIONES_HEIC = new Set(["heic", "heics", "heif", "heifs", "hif"]);
const EXTENSIONES_CONOCIDAS_NO_HEIC = new Set([
  "jpg",
  "jpeg",
  "png",
  "webp",
  "avif",
  "gif",
  "bmp",
  "tif",
  "tiff",
  "pdf",
]);
const MARCAS_HEIC = new Set(["heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs"]);

function extension(nombre: string) {
  const indice = nombre.lastIndexOf(".");
  return indice < 0 ? "" : nombre.slice(indice + 1).toLowerCase();
}

function nombreJpeg(nombre: string) {
  const indice = nombre.lastIndexOf(".");
  const base = indice > 0 ? nombre.slice(0, indice) : nombre || "evidencia";
  return `${base}.jpg`;
}

function ascii(bytes: Uint8Array, inicio: number, longitud: number) {
  return String.fromCharCode(...bytes.slice(inicio, inicio + longitud));
}

async function tieneFirmaHeic(archivo: File) {
  const bytes = new Uint8Array(await archivo.slice(0, 64).arrayBuffer());
  if (bytes.length < 12 || ascii(bytes, 4, 4) !== "ftyp") {
    return false;
  }

  for (let indice = 8; indice + 4 <= bytes.length; indice += 4) {
    if (indice === 12) {
      continue;
    }
    if (MARCAS_HEIC.has(ascii(bytes, indice, 4))) {
      return true;
    }
  }
  return false;
}

async function puedeSerHeic(archivo: File) {
  const mime = archivo.type.toLowerCase();
  if (MIME_HEIC.has(mime) || EXTENSIONES_HEIC.has(extension(archivo.name))) {
    return true;
  }

  // Una fotografia con MIME o extension de imagen conocidos debe continuar
  // directamente. Algunos proveedores de camara de Android entregan un File
  // valido para multipart, pero no permiten leerlo inmediatamente con
  // arrayBuffer(); esa lectura previa impediria incluso iniciar la carga.
  if (mime.startsWith("image/") || EXTENSIONES_CONOCIDAS_NO_HEIC.has(extension(archivo.name))) {
    return false;
  }

  try {
    return await tieneFirmaHeic(archivo);
  } catch {
    return false;
  }
}

function canvasAJpeg(canvas: HTMLCanvasElement) {
  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (resultado) => {
        if (resultado) {
          resolve(resultado);
          return;
        }
        reject(new Error("El navegador no pudo generar la fotografia JPEG."));
      },
      "image/jpeg",
      CALIDAD_JPEG,
    );
  });
}

async function convertirHeicAJpeg(archivo: File) {
  if (archivo.size > LIMITE_FOTO_HEIC_ORIGINAL) {
    throw new Error("La fotografia original supera el limite de 30 MB para conversion.");
  }

  try {
    const { heicTo, isHeic } = await import("heic-to");
    if (!(await isHeic(archivo))) {
      throw new Error("El contenido no corresponde a una fotografia HEIC o HEIF valida.");
    }

    const bitmap = await heicTo({ blob: archivo, type: "bitmap" });
    try {
      const escala = Math.min(1, LADO_MAXIMO_FOTO / Math.max(bitmap.width, bitmap.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(bitmap.width * escala));
      canvas.height = Math.max(1, Math.round(bitmap.height * escala));

      const contexto = canvas.getContext("2d");
      if (!contexto) {
        throw new Error("El navegador no permite procesar la fotografia seleccionada.");
      }
      contexto.drawImage(bitmap, 0, 0, canvas.width, canvas.height);

      const jpeg = await canvasAJpeg(canvas);
      if (jpeg.size > LIMITE_ARCHIVO_TRANSMITIDO) {
        throw new Error("La fotografia convertida supera el limite de 20 MB.");
      }
      return new File([jpeg], nombreJpeg(archivo.name), {
        lastModified: archivo.lastModified,
        type: "image/jpeg",
      });
    } finally {
      bitmap.close();
    }
  } catch (error) {
    const detalle = error instanceof Error ? error.message : "formato no reconocido";
    throw new Error(`No fue posible preparar la fotografia HEIC/HEIF: ${detalle}`);
  }
}

export async function prepararArchivoEvidencia(archivo: File) {
  if (await puedeSerHeic(archivo)) {
    try {
      return await convertirHeicAJpeg(archivo);
    } catch (error) {
      if (archivo.size <= LIMITE_ARCHIVO_TRANSMITIDO) {
        return archivo;
      }
      throw error;
    }
  }
  if (archivo.size > LIMITE_ARCHIVO_TRANSMITIDO) {
    throw new Error("La evidencia supera el limite de 20 MB.");
  }
  return archivo;
}
