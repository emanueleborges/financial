/** Formatação e parsing de valores monetários no padrão brasileiro. */

export function formatBRL(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(value);
}

/** Exibe número com milhar `.` e decimal `,` (sem símbolo R$). */
export function formatBRLInput(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return "";
  return new Intl.NumberFormat("pt-BR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/**
 * Converte digitação BR para número.
 * Aceita: "1.234,56", "1234,56", "1234.56", "R$ 1.234,56"
 */
export function parseBRLInput(raw: string): number | null {
  const cleaned = raw.replace(/[^\d.,]/g, "").trim();
  if (!cleaned) return null;

  const hasComma = cleaned.includes(",");
  const hasDot = cleaned.includes(".");

  let normalized = cleaned;
  if (hasComma && hasDot) {
    // 1.234,56 → remove pontos de milhar, vírgula vira decimal
    normalized = cleaned.replace(/\./g, "").replace(",", ".");
  } else if (hasComma) {
    normalized = cleaned.replace(",", ".");
  } else if (hasDot) {
    // Se só tem ponto: pode ser milhar (1.234) ou decimal US (1234.56)
    const parts = cleaned.split(".");
    if (parts.length === 2 && parts[1].length <= 2) {
      normalized = cleaned;
    } else {
      normalized = cleaned.replace(/\./g, "");
    }
  }

  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

/** Máscara progressiva enquanto digita (centavos implícitos estilo Pix). */
export function maskBRLFromDigits(raw: string): string {
  const digits = raw.replace(/\D/g, "");
  if (!digits) return "";

  const cents = Number(digits);
  if (!Number.isFinite(cents)) return "";

  return formatBRLInput(cents / 100);
}
