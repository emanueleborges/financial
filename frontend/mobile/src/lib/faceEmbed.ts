import * as ImageManipulator from "expo-image-manipulator";
import { decode } from "jpeg-js";

const FACE = 64;
export const MATCH_COSINE = 0.78;

type Raster = { width: number; height: number; gray: Float32Array; skin: number };

function decodeBase64Jpeg(base64: string) {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
  return decode(bytes, { useTArray: true });
}

function toRaster(raw: { width: number; height: number; data: Uint8Array }): Raster {
  const gray = new Float32Array(raw.width * raw.height);
  let skin = 0;
  for (let i = 0, p = 0; i < raw.data.length; i += 4, p += 1) {
    const r = raw.data[i];
    const g = raw.data[i + 1];
    const b = raw.data[i + 2];
    gray[p] = r * 0.299 + g * 0.587 + b * 0.114;
    const max = Math.max(r, g, b);
    const min = Math.min(r, g, b);
    if (r > 80 && r > g && r > b && max - min > 15) skin += 1;
  }
  return { width: raw.width, height: raw.height, gray, skin: skin / (raw.width * raw.height) };
}

function cropCenter(src: Raster, size: number): Float32Array {
  const side = Math.floor(Math.min(src.width, src.height) * 0.72);
  const x0 = Math.floor((src.width - side) / 2);
  const y0 = Math.floor((src.height - side) / 2);
  const out = new Float32Array(size * size);
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const sx = x0 + Math.floor((x / size) * side);
      const sy = y0 + Math.floor((y / size) * side);
      out[y * size + x] = src.gray[sy * src.width + sx];
    }
  }
  let min = Infinity;
  let max = -Infinity;
  for (const v of out) {
    if (v < min) min = v;
    if (v > max) max = v;
  }
  const span = Math.max(1, max - min);
  for (let i = 0; i < out.length; i += 1) out[i] = (out[i] - min) / span;
  return out;
}

function lbpHist(gray: Float32Array, size: number) {
  const hist = new Float32Array(59);
  const uniform = (code: number) => {
    const wrapped = ((code << 1) | (code >> 7)) & 255;
    const transitions = (code ^ wrapped).toString(2).replace(/0/g, "").length;
    return transitions <= 2;
  };
  const map = new Int16Array(256).fill(58);
  let next = 0;
  for (let i = 0; i < 256; i += 1) {
    if (uniform(i)) {
      map[i] = next;
      next += 1;
    }
  }
  const offs = [
    [-1, -1],
    [0, -1],
    [1, -1],
    [1, 0],
    [1, 1],
    [0, 1],
    [-1, 1],
    [-1, 0],
  ];
  let count = 0;
  for (let y = 1; y < size - 1; y += 1) {
    for (let x = 1; x < size - 1; x += 1) {
      const c = gray[y * size + x];
      let code = 0;
      offs.forEach(([dx, dy], bit) => {
        if (gray[(y + dy) * size + (x + dx)] >= c) code |= 1 << bit;
      });
      hist[map[code]] += 1;
      count += 1;
    }
  }
  for (let i = 0; i < hist.length; i += 1) hist[i] /= count;
  return hist;
}

function hog(gray: Float32Array, size: number, cells = 4, bins = 8) {
  const cell = size / cells;
  const hist = new Float32Array(cells * cells * bins);
  for (let y = 1; y < size - 1; y += 1) {
    for (let x = 1; x < size - 1; x += 1) {
      const gx = gray[y * size + x + 1] - gray[y * size + x - 1];
      const gy = gray[(y + 1) * size + x] - gray[(y - 1) * size + x];
      const mag = Math.hypot(gx, gy);
      let ang = (Math.atan2(gy, gx) + Math.PI) / (2 * Math.PI);
      if (ang >= 1) ang = 0;
      const bin = Math.min(bins - 1, Math.floor(ang * bins));
      const cx = Math.min(cells - 1, Math.floor(x / cell));
      const cy = Math.min(cells - 1, Math.floor(y / cell));
      hist[(cy * cells + cx) * bins + bin] += mag;
    }
  }
  let norm = 0;
  for (const v of hist) norm += v * v;
  norm = Math.sqrt(norm) || 1;
  for (let i = 0; i < hist.length; i += 1) hist[i] /= norm;
  return hist;
}

function gaborBank(gray: Float32Array, size: number) {
  const thetas = [0, Math.PI / 4, Math.PI / 2, (3 * Math.PI) / 4];
  const freqs = [0.18, 0.32];
  const out = new Float32Array(thetas.length * freqs.length * 8);
  let o = 0;
  for (const theta of thetas) {
    for (const freq of freqs) {
      const blocks = new Float32Array(4);
      const counts = new Float32Array(4);
      const k = 2;
      for (let y = k; y < size - k; y += 1) {
        for (let x = k; x < size - k; x += 1) {
          let acc = 0;
          for (let ky = -k; ky <= k; ky += 1) {
            for (let kx = -k; kx <= k; kx += 1) {
              const xr = kx * Math.cos(theta) + ky * Math.sin(theta);
              const yr = -kx * Math.sin(theta) + ky * Math.cos(theta);
              const gauss = Math.exp(-(xr * xr + yr * yr) / 8);
              acc += gray[(y + ky) * size + (x + kx)] * gauss * Math.cos((2 * Math.PI * freq * xr));
            }
          }
          const q = (y < size / 2 ? 0 : 2) + (x < size / 2 ? 0 : 1);
          blocks[q] += Math.abs(acc);
          counts[q] += 1;
        }
      }
      for (let q = 0; q < 4; q += 1) {
        const mean = blocks[q] / (counts[q] || 1);
        out[o] = mean;
        o += 1;
        out[o] = mean * mean;
        o += 1;
      }
    }
  }
  let norm = 0;
  for (const v of out) norm += v * v;
  norm = Math.sqrt(norm) || 1;
  for (let i = 0; i < out.length; i += 1) out[i] /= norm;
  return out;
}

function concat(...parts: Float32Array[]) {
  const total = parts.reduce((sum, part) => sum + part.length, 0);
  const out = new Float32Array(total);
  let offset = 0;
  for (const part of parts) {
    out.set(part, offset);
    offset += part.length;
  }
  let norm = 0;
  for (const v of out) norm += v * v;
  norm = Math.sqrt(norm) || 1;
  for (let i = 0; i < out.length; i += 1) out[i] /= norm;
  return Array.from(out);
}

export function cosineSimilarity(a: number[], b: number[]) {
  const n = Math.min(a.length, b.length);
  let dot = 0;
  let na = 0;
  let nb = 0;
  for (let i = 0; i < n; i += 1) {
    dot += a[i] * b[i];
    na += a[i] * a[i];
    nb += b[i] * b[i];
  }
  return dot / ((Math.sqrt(na) || 1) * (Math.sqrt(nb) || 1));
}

export async function embedFace(uri: string) {
  const resized = await ImageManipulator.manipulateAsync(uri, [{ resize: { width: 160, height: 160 } }], {
    compress: 1,
    format: ImageManipulator.SaveFormat.JPEG,
    base64: true,
  });
  if (!resized.base64) throw new Error("Não foi possível ler a foto do rosto.");
  const raster = toRaster(decodeBase64Jpeg(resized.base64));
  if (raster.skin < 0.08) {
    throw new Error("Nenhum rosto nítido na câmera. Enquadre o rosto e tente de novo.");
  }
  const face = cropCenter(raster, FACE);
  return concat(lbpHist(face, FACE), hog(face, FACE), gaborBank(face, FACE));
}

export function embeddingsMatch(enrolled: number[], probe: number[]) {
  return cosineSimilarity(enrolled, probe) >= MATCH_COSINE;
}
