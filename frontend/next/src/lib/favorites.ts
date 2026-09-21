export type FavoritePayee = {
  document: string;
  name: string;
  savedAt: string;
};

const STORAGE_PREFIX = "fh_favorites_";

function storageKey(ownerDocument: string) {
  return `${STORAGE_PREFIX}${ownerDocument}`;
}

function onlyDigits(value: string) {
  return value.replace(/\D/g, "");
}

export function listFavorites(ownerDocument: string): FavoritePayee[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(storageKey(ownerDocument));
    if (!raw) return [];
    const parsed = JSON.parse(raw) as FavoritePayee[];
    if (!Array.isArray(parsed)) return [];
    return parsed
      .filter((item) => item?.document && item?.name)
      .map((item) => ({
        document: onlyDigits(item.document),
        name: item.name,
        savedAt: item.savedAt ?? new Date().toISOString(),
      }))
      .sort((a, b) => b.savedAt.localeCompare(a.savedAt));
  } catch {
    return [];
  }
}

export function isFavorite(ownerDocument: string, payeeDocument: string): boolean {
  const digits = onlyDigits(payeeDocument);
  return listFavorites(ownerDocument).some((item) => item.document === digits);
}

export function addFavorite(
  ownerDocument: string,
  payee: { document: string; name: string }
): FavoritePayee[] {
  const digits = onlyDigits(payee.document);
  if (!digits || digits === onlyDigits(ownerDocument)) {
    return listFavorites(ownerDocument);
  }

  const current = listFavorites(ownerDocument).filter((item) => item.document !== digits);
  const next: FavoritePayee[] = [
    {
      document: digits,
      name: payee.name.trim() || digits,
      savedAt: new Date().toISOString(),
    },
    ...current,
  ].slice(0, 30);

  localStorage.setItem(storageKey(ownerDocument), JSON.stringify(next));
  return next;
}

export function removeFavorite(ownerDocument: string, payeeDocument: string): FavoritePayee[] {
  const digits = onlyDigits(payeeDocument);
  const next = listFavorites(ownerDocument).filter((item) => item.document !== digits);
  localStorage.setItem(storageKey(ownerDocument), JSON.stringify(next));
  return next;
}

export function toggleFavorite(
  ownerDocument: string,
  payee: { document: string; name: string }
): FavoritePayee[] {
  if (isFavorite(ownerDocument, payee.document)) {
    return removeFavorite(ownerDocument, payee.document);
  }
  return addFavorite(ownerDocument, payee);
}
