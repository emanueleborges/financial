"use client";

import { FormEvent, useEffect, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import {
  ApiClientError,
  getUser,
  transfer,
  type TransactionResponse,
  type UserResponse,
} from "@/lib/api";
import {
  addFavorite,
  isFavorite,
  listFavorites,
  removeFavorite,
  type FavoritePayee,
} from "@/lib/favorites";
import { formatBRL, parseBRLInput } from "@/lib/money";
import { MoneyInput } from "@/components/money-input";
import {
  FormError,
  Field,
  inputClass,
  inputErrorClass,
  buttonPrimaryClass,
  buttonGhostClass,
} from "@/components/ui";

function onlyDigits(value: string) {
  return value.replace(/\D/g, "");
}

function isValidDocumentLength(digits: string) {
  return digits.length === 11 || digits.length === 14;
}

function documentKind(digits: string) {
  return digits.length === 14 ? "CNPJ" : "CPF";
}

type PayeeMode = "favorite" | "new";

type PayeeLookup =
  | { status: "idle" }
  | { status: "loading"; document: string }
  | { status: "found"; user: UserResponse }
  | { status: "not_found"; document: string }
  | { status: "self"; document: string }
  | { status: "error"; message: string };

export default function TransferPage() {
  const { session } = useAuth();
  const [favorites, setFavorites] = useState<FavoritePayee[]>([]);
  const [mode, setMode] = useState<PayeeMode>("new");
  const [payeeDocument, setPayeeDocument] = useState("");
  const [amountMasked, setAmountMasked] = useState("");
  const [amountValue, setAmountValue] = useState<number | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<TransactionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [payeeLookup, setPayeeLookup] = useState<PayeeLookup>({ status: "idle" });

  useEffect(() => {
    if (!session) return;
    const saved = listFavorites(session.document);
    setFavorites(saved);
    if (saved.length > 0) {
      setMode("favorite");
      setPayeeDocument(saved[0].document);
    } else {
      setMode("new");
    }
  }, [session]);

  useEffect(() => {
    if (!session) return;

    const digits = onlyDigits(payeeDocument);
    if (!isValidDocumentLength(digits)) {
      setPayeeLookup({ status: "idle" });
      return;
    }

    if (digits === session.document) {
      setPayeeLookup({ status: "self", document: digits });
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(async () => {
      setPayeeLookup({ status: "loading", document: digits });
      try {
        const user = await getUser(digits, session.token);
        if (cancelled) return;
        setPayeeLookup({ status: "found", user });
        setFieldErrors((prev) => {
          const next = { ...prev };
          delete next.payeeDocument;
          return next;
        });
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiClientError && err.status === 404) {
          setPayeeLookup({ status: "not_found", document: digits });
        } else {
          setPayeeLookup({
            status: "error",
            message: err instanceof Error ? err.message : "Falha ao consultar recebedor",
          });
        }
      }
    }, 400);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [payeeDocument, session]);

  function selectFavorite(item: FavoritePayee) {
    setMode("favorite");
    setPayeeDocument(item.document);
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next.payeeDocument;
      return next;
    });
  }

  function switchToNew() {
    setMode("new");
    setPayeeDocument("");
    setPayeeLookup({ status: "idle" });
    setFieldErrors((prev) => {
      const next = { ...prev };
      delete next.payeeDocument;
      return next;
    });
  }

  function saveCurrentAsFavorite() {
    if (!session || payeeLookup.status !== "found") return;
    const next = addFavorite(session.document, {
      document: payeeLookup.user.document,
      name: payeeLookup.user.name,
    });
    setFavorites(next);
  }

  function unfavoriteCurrent() {
    if (!session || payeeLookup.status !== "found") return;
    const next = removeFavorite(session.document, payeeLookup.user.document);
    setFavorites(next);
    if (next.length === 0) {
      setMode("new");
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!session) return;

    const digits = onlyDigits(payeeDocument);
    const amount = amountValue ?? parseBRLInput(amountMasked);
    const local: Record<string, string> = {};

    if (!isValidDocumentLength(digits)) {
      local.payeeDocument = "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos)";
    } else if (digits === session.document) {
      local.payeeDocument = "Não é possível transferir para o próprio CPF/CNPJ";
    } else if (payeeLookup.status === "not_found") {
      local.payeeDocument = `${documentKind(digits)} não cadastrado`;
    } else if (payeeLookup.status === "loading" || payeeLookup.status === "idle") {
      local.payeeDocument = "Aguarde a identificação do recebedor";
    } else if (payeeLookup.status !== "found") {
      local.payeeDocument = "Não foi possível identificar o recebedor";
    }

    if (amount == null || amount <= 0) {
      local.amount = "Valor deve ser maior que zero";
    }

    setFieldErrors(local);
    if (Object.keys(local).length > 0) {
      setError(
        new ApiClientError(400, {
          code: "VALIDATION_ERROR",
          message: "Um ou mais campos são inválidos",
          fields: local,
        })
      );
      return;
    }

    setError(null);
    setResult(null);
    setLoading(true);
    try {
      const tx = await transfer(
        {
          payerDocument: session.document,
          payeeDocument: digits,
          amount: amount as number,
        },
        session.token,
        crypto.randomUUID()
      );
      setResult(tx);
      setAmountMasked("");
      setAmountValue(null);
      setFieldErrors({});
    } catch (err) {
      setError(err);
      if (err instanceof ApiClientError) {
        setFieldErrors(err.fields);
      }
    } finally {
      setLoading(false);
    }
  }

  const payeeReady = payeeLookup.status === "found";
  const favorited =
    session && payeeLookup.status === "found"
      ? isFavorite(session.document, payeeLookup.user.document)
      : false;
  const payeeFieldError =
    fieldErrors.payeeDocument ||
    (payeeLookup.status === "not_found"
      ? `${documentKind(payeeLookup.document)} não cadastrado`
      : payeeLookup.status === "self"
        ? "Não é possível transferir para o próprio CPF/CNPJ"
        : payeeLookup.status === "error"
          ? payeeLookup.message
          : undefined);

  return (
    <div className="mx-auto max-w-lg space-y-8">
      <div>
        <p className="text-xs uppercase tracking-[0.16em] text-white/45">P2P</p>
        <h1 className="mt-2 font-[family-name:var(--font-display)] text-4xl">
          Transferir
        </h1>
        <p className="mt-2 text-sm text-white/55">
          Selecione um CPF/CNPJ favorito ou informe um novo. Limite diário: R$ 5.000,00.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => {
            if (favorites.length === 0) return;
            setMode("favorite");
            setPayeeDocument(favorites[0].document);
          }}
          disabled={favorites.length === 0}
          className={`${buttonGhostClass} ${
            mode === "favorite" ? "border-[var(--amber)]/50 bg-[var(--amber)]/10" : ""
          }`}
        >
          Favoritos {favorites.length > 0 ? `(${favorites.length})` : ""}
        </button>
        <button
          type="button"
          onClick={switchToNew}
          className={`${buttonGhostClass} ${
            mode === "new" ? "border-[var(--amber)]/50 bg-[var(--amber)]/10" : ""
          }`}
        >
          Novo CPF / CNPJ
        </button>
      </div>

      {mode === "favorite" && (
        <div className="space-y-2">
          <p className="text-xs uppercase tracking-[0.14em] text-white/45">Recebedores favoritos</p>
          {favorites.length === 0 ? (
            <p className="rounded-xl border border-white/10 bg-white/[0.03] px-4 py-5 text-sm text-white/55">
              Nenhum favorito ainda. Use “Novo CPF / CNPJ” ou favoritize em Transações.
            </p>
          ) : (
            <ul className="space-y-2">
              {favorites.map((item) => {
                const active = onlyDigits(payeeDocument) === item.document;
                return (
                  <li key={item.document}>
                    <button
                      type="button"
                      onClick={() => selectFavorite(item)}
                      className={`w-full rounded-xl border px-4 py-3 text-left transition ${
                        active
                          ? "border-[var(--amber)]/50 bg-[var(--amber)]/10"
                          : "border-white/10 bg-white/[0.03] hover:bg-white/[0.06]"
                      }`}
                    >
                      <p className="font-[family-name:var(--font-display)] text-lg text-[var(--foam)]">
                        {item.name}
                      </p>
                      <p className="mt-0.5 font-mono text-xs text-white/50">
                        {documentKind(item.document)} {item.document}
                      </p>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-5" noValidate>
        {mode === "new" && (
          <Field label="CPF / CNPJ do recebedor" error={payeeFieldError}>
            <input
              className={payeeFieldError ? inputErrorClass : inputClass}
              required
              inputMode="numeric"
              maxLength={18}
              value={payeeDocument}
              onChange={(e) => {
                setPayeeDocument(e.target.value);
                setFieldErrors((prev) => {
                  const next = { ...prev };
                  delete next.payeeDocument;
                  return next;
                });
              }}
              placeholder="39053344705"
              aria-invalid={Boolean(payeeFieldError)}
            />
          </Field>
        )}

        {mode === "favorite" && payeeFieldError && (
          <p className="text-xs text-red-300">{payeeFieldError}</p>
        )}

        {payeeLookup.status === "loading" && (
          <p className="text-xs text-white/45">Identificando recebedor…</p>
        )}

        {payeeLookup.status === "found" && (
          <div className="rounded-lg border border-[var(--teal)]/35 bg-[var(--teal)]/10 px-3 py-2.5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.12em] text-white/45">
                  Recebedor identificado
                </p>
                <p className="mt-1 font-[family-name:var(--font-display)] text-lg text-[var(--foam)]">
                  {payeeLookup.user.name}
                </p>
                <p className="mt-0.5 font-mono text-xs text-white/55">
                  {documentKind(payeeLookup.user.document)} {payeeLookup.user.document}
                </p>
              </div>
              <button
                type="button"
                onClick={favorited ? unfavoriteCurrent : saveCurrentAsFavorite}
                className="shrink-0 rounded-lg border border-white/20 px-3 py-2 text-xs text-white/80 transition hover:bg-white/5"
              >
                {favorited ? "Remover favorito" : "Favoritar"}
              </button>
            </div>
          </div>
        )}

        <Field label="Valor" error={fieldErrors.amount}>
          <MoneyInput
            required
            error={Boolean(fieldErrors.amount)}
            value={amountMasked}
            placeholder="0,00"
            onChange={(masked, numeric) => {
              setAmountMasked(masked);
              setAmountValue(numeric);
              setFieldErrors((prev) => {
                const next = { ...prev };
                delete next.amount;
                return next;
              });
            }}
          />
        </Field>
        <FormError error={error} onDismiss={() => setError(null)} />
        <button
          type="submit"
          disabled={loading || !payeeReady}
          className={buttonPrimaryClass}
        >
          {loading ? "Enviando…" : "Confirmar transferência"}
        </button>
      </form>

      {result && (
        <div className="rounded-xl border border-[var(--teal)]/40 bg-[var(--teal)]/15 p-5 text-sm">
          <p className="text-[var(--amber)]">Transferência {result.status}</p>
          <p className="mt-2 font-[family-name:var(--font-display)] text-2xl">
            {formatBRL(result.amount)}
          </p>
          {(result.payeeName || result.payeeDocument) && (
            <p className="mt-2 text-sm text-white/65">
              Para {result.payeeName ?? "—"}
              {result.payeeDocument ? ` · ${result.payeeDocument}` : ""}
            </p>
          )}
          <p className="mt-3 break-all font-mono text-xs text-white/50">
            TX: {result.id}
          </p>
        </div>
      )}
    </div>
  );
}
