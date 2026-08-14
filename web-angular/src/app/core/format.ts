export function formatBRL(value: number) {
  return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

export function formatBRLInput(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return '';
  return new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function parseBRLInput(raw: string): number | null {
  const cleaned = raw.replace(/[^\d.,]/g, '').trim();
  if (!cleaned) return null;
  const hasComma = cleaned.includes(',');
  const hasDot = cleaned.includes('.');
  let normalized = cleaned;
  if (hasComma && hasDot) {
    normalized = cleaned.replace(/\./g, '').replace(',', '.');
  } else if (hasComma) {
    normalized = cleaned.replace(',', '.');
  } else if (hasDot) {
    const parts = cleaned.split('.');
    if (!(parts.length === 2 && parts[1].length <= 2)) {
      normalized = cleaned.replace(/\./g, '');
    }
  }
  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

export function maskBRLFromDigits(raw: string): string {
  const digits = raw.replace(/\D/g, '');
  if (!digits) return '';
  return formatBRLInput(Number(digits) / 100);
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('pt-BR');
}

export function formatDateTimeShort(value: string | null | undefined): string {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export function onlyDigits(value: string) {
  return value.replace(/\D/g, '');
}

export function isValidDocumentLength(digits: string) {
  return digits.length === 11 || digits.length === 14;
}

export function documentKind(digits: string) {
  return digits.length === 14 ? 'CNPJ' : 'CPF';
}

export function formatSigned(value: number) {
  const absolute = formatBRL(Math.abs(value));
  if (value > 0) return `+ ${absolute}`;
  if (value < 0) return `− ${absolute}`;
  return absolute;
}

export function fieldLabel(field: string) {
  const labels: Record<string, string> = {
    name: 'Nome',
    email: 'E-mail',
    document: 'CPF / CNPJ',
    password: 'Senha',
    initialBalance: 'Saldo inicial',
    payerDocument: 'CPF / CNPJ do pagador',
    payeeDocument: 'CPF / CNPJ do recebedor',
    amount: 'Valor',
  };
  return labels[field] ?? field;
}
