"use client";

import { ApiClientError } from "@/lib/api";
import { useEffect, useRef, type ReactNode } from "react";

const FIELD_LABELS: Record<string, string> = {
  name: "Nome",
  email: "E-mail",
  document: "CPF / CNPJ",
  password: "Senha",
  initialBalance: "Saldo inicial",
  payerId: "Pagador",
  payeeId: "Recebedor",
  payerDocument: "CPF / CNPJ do pagador",
  payeeDocument: "CPF / CNPJ do recebedor",
  amount: "Valor",
  transactionId: "Transação",
  reason: "Motivo",
};

export function fieldLabel(field: string) {
  return FIELD_LABELS[field] ?? field;
}

export function FormError({
  error,
  onDismiss,
  dismissAfterMs = 5000,
}: {
  error: unknown;
  onDismiss?: () => void;
  dismissAfterMs?: number;
}) {
  const onDismissRef = useRef(onDismiss);
  onDismissRef.current = onDismiss;

  useEffect(() => {
    if (!error || !onDismissRef.current) return;
    const timer = window.setTimeout(() => {
      onDismissRef.current?.();
    }, dismissAfterMs);
    return () => window.clearTimeout(timer);
  }, [error, dismissAfterMs]);

  if (!error) return null;

  if (error instanceof ApiClientError && Object.keys(error.fields).length > 0) {
    return (
      <div
        role="alert"
        className="rounded-lg border border-red-400/30 bg-red-500/10 px-4 py-3 text-sm text-red-100"
      >
        <p className="font-medium">{error.message}</p>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-red-100/90">
          {Object.entries(error.fields).map(([field, message]) => (
            <li key={field}>
              <span className="font-semibold">{fieldLabel(field)}</span>: {message}
            </li>
          ))}
        </ul>
      </div>
    );
  }

  const message =
    error instanceof ApiClientError
      ? `${error.code}: ${error.message}`
      : error instanceof Error
        ? error.message
        : "Erro inesperado";

  return (
    <div
      role="alert"
      className="rounded-lg border border-red-400/30 bg-red-500/10 px-4 py-3 text-sm text-red-100"
    >
      {message}
    </div>
  );
}

export function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className="text-xs text-red-300">{message}</p>;
}

export function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <label className="block space-y-2">
      <span className="text-xs font-medium uppercase tracking-[0.14em] text-white/50">
        {label}
      </span>
      {children}
      <FieldError message={error} />
    </label>
  );
}

export const inputClass =
  "w-full rounded-lg border border-white/15 bg-white/5 px-4 py-3 text-[var(--foam)] outline-none transition placeholder:text-white/30 focus:border-[var(--amber)]/60 focus:bg-white/10";

export const inputErrorClass =
  "w-full rounded-lg border border-red-400/50 bg-red-500/5 px-4 py-3 text-[var(--foam)] outline-none transition placeholder:text-white/30 focus:border-red-300/70";

export const buttonPrimaryClass =
  "inline-flex items-center justify-center rounded-lg bg-[var(--amber)] px-5 py-3 text-sm font-semibold text-[var(--ink)] transition hover:bg-[#f0b84a] disabled:cursor-not-allowed disabled:opacity-50";

export const buttonGhostClass =
  "inline-flex items-center justify-center rounded-lg border border-white/20 px-5 py-3 text-sm text-white/80 transition hover:bg-white/5";
