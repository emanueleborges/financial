"use client";

import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import {
  downloadReceipt,
  downloadStatementPdf,
  listUserTransactions,
  reverseTransaction,
  triggerBlobDownload,
  type StatementEntryResponse,
  type TransactionResponse,
} from "@/lib/api";
import {
  isFavorite,
  listFavorites,
  toggleFavorite,
  type FavoritePayee,
} from "@/lib/favorites";
import { formatBRL } from "@/lib/money";
import { formatDateTime, formatDateTimeShort } from "@/lib/datetime";
import {
  FormError,
  Field,
  inputClass,
  buttonGhostClass,
} from "@/components/ui";

export default function TransactionsPage() {
  const { session } = useAuth();
  const [entries, setEntries] = useState<StatementEntryResponse[]>([]);
  const [currentBalance, setCurrentBalance] = useState<number | null>(null);
  const [favorites, setFavorites] = useState<FavoritePayee[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [reason, setReason] = useState("Estorno solicitado pelo usuário");
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [reversing, setReversing] = useState(false);
  const [downloadingReceipt, setDownloadingReceipt] = useState(false);
  const [downloadingStatement, setDownloadingStatement] = useState(false);

  const load = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    setError(null);
    try {
      const statement = await listUserTransactions(session.document, session.token, 50);
      setEntries(statement.entries);
      setCurrentBalance(statement.currentBalance);
      setFavorites(listFavorites(session.document));
      setSelectedId((current) => {
        if (current && statement.entries.some((e) => e.transaction.id === current)) {
          return current;
        }
        return statement.entries[0]?.transaction.id ?? null;
      });
    } catch (err) {
      setError(err);
      setEntries([]);
      setCurrentBalance(null);
    } finally {
      setLoading(false);
    }
  }, [session]);

  useEffect(() => {
    void load();
  }, [load]);

  const selectedEntry = entries.find((e) => e.transaction.id === selectedId) ?? null;
  const selected = selectedEntry?.transaction ?? null;

  const canReverse =
    selected?.type === "TRANSFER" &&
    selected?.status === "COMPLETED" &&
    selected.payerDocument === session?.document;

  const counterparty =
    selected && session
      ? selected.payerDocument === session.document
        ? {
            document: selected.payeeDocument,
            name: selected.payeeName,
          }
        : {
            document: selected.payerDocument,
            name: selected.payerName,
          }
      : null;

  const canFavorite =
    Boolean(session) &&
    Boolean(counterparty?.document) &&
    counterparty?.document !== session?.document;

  const counterpartyFavorited =
    session && counterparty?.document
      ? isFavorite(session.document, counterparty.document)
      : false;

  async function onReverse() {
    if (!session || !selected) return;
    setError(null);
    setReversing(true);
    try {
      const reversal = await reverseTransaction(
        { transactionId: selected.id, reason },
        session.token
      );
      await load();
      setSelectedId(reversal.id);
    } catch (err) {
      setError(err);
    } finally {
      setReversing(false);
    }
  }

  function onToggleFavorite() {
    if (!session || !counterparty?.document) return;
    const next = toggleFavorite(session.document, {
      document: counterparty.document,
      name: counterparty.name ?? counterparty.document,
    });
    setFavorites(next);
  }

  async function onDownloadReceipt() {
    if (!session || !selected) return;
    setError(null);
    setDownloadingReceipt(true);
    try {
      const blob = await downloadReceipt(selected.id, session.token);
      triggerBlobDownload(blob, `comprovante-${selected.id}.pdf`);
    } catch (err) {
      setError(err);
    } finally {
      setDownloadingReceipt(false);
    }
  }

  async function onDownloadStatement() {
    if (!session) return;
    setError(null);
    setDownloadingStatement(true);
    try {
      const blob = await downloadStatementPdf(session.document, session.token, 50);
      triggerBlobDownload(blob, `extrato-${session.document}.pdf`);
    } catch (err) {
      setError(err);
    } finally {
      setDownloadingStatement(false);
    }
  }

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.16em] text-white/45">Conta</p>
          <h1 className="mt-2 font-[family-name:var(--font-display)] text-4xl">
            Extrato
          </h1>
          <p className="mt-2 text-sm text-white/55">
            Movimentações com saldo após cada lançamento.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => void onDownloadStatement()}
            disabled={downloadingStatement || entries.length === 0}
            className={buttonGhostClass}
          >
            {downloadingStatement ? "Gerando PDF…" : "Exportar extrato PDF"}
          </button>
          <button type="button" onClick={() => void load()} className={buttonGhostClass}>
            Atualizar
          </button>
        </div>
      </div>

      <section className="flex flex-wrap items-baseline justify-between gap-3 rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3">
        <div>
          <p className="text-[10px] uppercase tracking-[0.14em] text-white/45">Saldo atual</p>
          <p className="mt-1 font-[family-name:var(--font-display)] text-xl text-[var(--amber)] sm:text-2xl">
            {currentBalance == null ? "—" : formatBRL(currentBalance)}
          </p>
        </div>
        {session?.document && (
          <p className="font-mono text-[11px] text-white/35">{session.document}</p>
        )}
      </section>

      <FormError error={error} onDismiss={() => setError(null)} />

      {loading && entries.length === 0 ? (
        <p className="text-white/50">Carregando extrato…</p>
      ) : entries.length === 0 ? (
        <p className="rounded-xl border border-white/10 bg-white/[0.03] px-5 py-8 text-center text-white/55">
          Nenhuma movimentação ainda. Faça uma transferência para ver o extrato aqui.
        </p>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[1.25fr_0.75fr]">
          <div className="overflow-hidden rounded-xl border border-white/10">
            <div className="grid grid-cols-[1.2fr_0.9fr_0.9fr_0.9fr] gap-2 border-b border-white/10 bg-white/[0.04] px-4 py-3 text-[10px] uppercase tracking-[0.12em] text-white/40 sm:grid-cols-[1.4fr_1fr_1fr_1fr]">
              <span>Movimentação</span>
              <span className="text-right">Valor</span>
              <span className="text-right">Saldo</span>
              <span className="hidden text-right sm:block">Quando</span>
            </div>
            <ul>
              {entries.map((entry) => {
                const tx = entry.transaction;
                const active = tx.id === selectedId;
                return (
                  <li key={tx.id} className="border-b border-white/5 last:border-b-0">
                    <button
                      type="button"
                      onClick={() => setSelectedId(tx.id)}
                      className={`grid w-full grid-cols-[1.2fr_0.9fr_0.9fr_0.9fr] gap-2 px-4 py-3.5 text-left transition sm:grid-cols-[1.4fr_1fr_1fr_1fr] ${
                        active
                          ? "bg-[var(--amber)]/10"
                          : "hover:bg-white/[0.04]"
                      }`}
                    >
                      <div className="min-w-0">
                        <p className="truncate text-sm text-[var(--foam)]">
                          {movementLabel(tx, session?.document)}
                        </p>
                        <p className="mt-0.5 text-[11px] text-white/40">
                          {tx.type} · {tx.status}
                        </p>
                      </div>
                      <p
                        className={`text-right text-sm font-medium ${
                          entry.signedAmount < 0
                            ? "text-red-200"
                            : entry.signedAmount > 0
                              ? "text-emerald-200"
                              : "text-white/70"
                        }`}
                      >
                        {formatSigned(entry.signedAmount)}
                      </p>
                      <p className="text-right font-mono text-sm text-white/75">
                        {entry.balanceAfter == null ? "—" : formatBRL(entry.balanceAfter)}
                      </p>
                      <p className="hidden text-right text-[11px] text-white/40 sm:block">
                        {formatDateTimeShort(tx.createdAt)}
                      </p>
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>

          {selected && selectedEntry && (
            <div className="h-fit space-y-4 rounded-xl border border-white/10 bg-white/[0.03] p-5">
              <dl className="grid gap-3 text-sm">
                <Row label="Status" value={selected.status} highlight />
                <Row label="Tipo" value={selected.type} />
                <Row label="Valor" value={formatSigned(selectedEntry.signedAmount)} />
                <Row
                  label="Saldo após"
                  value={
                    selectedEntry.balanceAfter == null
                      ? "—"
                      : formatBRL(selectedEntry.balanceAfter)
                  }
                />
                <Party
                  label="Pagador"
                  document={selected.payerDocument}
                  name={selected.payerName}
                />
                <Party
                  label="Recebedor"
                  document={selected.payeeDocument}
                  name={selected.payeeName}
                />
                <Row
                  label="Criada em"
                  value={formatDateTime(selected.createdAt)}
                />
                {selected.failureReason && (
                  <Row label="Falha" value={selected.failureReason} />
                )}
                <Row label="ID" value={selected.id} mono />
              </dl>

              <div className="space-y-3 border-t border-white/10 pt-4">
                <button
                  type="button"
                  onClick={() => void onDownloadReceipt()}
                  disabled={downloadingReceipt}
                  className={buttonGhostClass}
                >
                  {downloadingReceipt ? "Gerando PDF…" : "Exportar comprovante PDF"}
                </button>

                {canFavorite && (
                  <>
                    <button
                      type="button"
                      onClick={onToggleFavorite}
                      className={buttonGhostClass}
                    >
                      {counterpartyFavorited
                        ? "Remover dos favoritos"
                        : `Favoritar ${counterparty?.name ?? "CPF/CNPJ"}`}
                    </button>
                    {favorites.length > 0 && (
                      <p className="text-xs text-white/40">
                        {favorites.length} favorito{favorites.length > 1 ? "s" : ""} salvos neste
                        navegador.
                      </p>
                    )}
                  </>
                )}
              </div>

              {canReverse && (
                <div className="space-y-3 border-t border-white/10 pt-4">
                  <Field label="Motivo do estorno">
                    <input
                      className={inputClass}
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                    />
                  </Field>
                  <button
                    type="button"
                    onClick={() => void onReverse()}
                    disabled={reversing}
                    className={buttonGhostClass}
                  >
                    {reversing ? "Estornando…" : "Estornar transferência"}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function formatSigned(value: number) {
  const absolute = formatBRL(Math.abs(value));
  if (value > 0) return `+ ${absolute}`;
  if (value < 0) return `− ${absolute}`;
  return absolute;
}

function movementLabel(tx: TransactionResponse, viewerDocument?: string) {
  const outbound = tx.payerDocument === viewerDocument;
  if (tx.type === "REVERSAL") {
    return outbound
      ? `Estorno para ${tx.payeeName ?? tx.payeeDocument ?? "—"}`
      : `Estorno de ${tx.payerName ?? tx.payerDocument ?? "—"}`;
  }
  return outbound
    ? `Transferência para ${tx.payeeName ?? tx.payeeDocument ?? "—"}`
    : `Transferência de ${tx.payerName ?? tx.payerDocument ?? "—"}`;
}

function Party({
  label,
  document,
  name,
}: {
  label: string;
  document: string | null;
  name: string | null;
}) {
  return (
    <div>
      <dt className="text-white/40">{label}</dt>
      <dd className="mt-1">
        <p className="font-mono text-sm text-[var(--foam)]">
          {document ?? "—"}
        </p>
        <p className="mt-0.5 text-sm text-white/65">{name ?? "—"}</p>
      </dd>
    </div>
  );
}

function Row({
  label,
  value,
  mono,
  highlight,
}: {
  label: string;
  value: string;
  mono?: boolean;
  highlight?: boolean;
}) {
  return (
    <div>
      <dt className="text-white/40">{label}</dt>
      <dd
        className={`mt-1 break-all ${mono ? "font-mono text-xs" : ""} ${
          highlight ? "text-[var(--amber)]" : "text-[var(--foam)]"
        }`}
      >
        {value}
      </dd>
    </div>
  );
}
