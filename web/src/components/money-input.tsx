"use client";

import { maskBRLFromDigits, parseBRLInput } from "@/lib/money";
import { inputClass, inputErrorClass } from "@/components/ui";

type MoneyInputProps = {
  value: string;
  onChange: (masked: string, numeric: number | null) => void;
  error?: boolean;
  placeholder?: string;
  id?: string;
  name?: string;
  required?: boolean;
  min?: number;
};

/**
 * Input monetário BR: digita centavos e formata como 1.234,56
 */
export function MoneyInput({
  value,
  onChange,
  error,
  placeholder = "0,00",
  id,
  name,
  required,
  min = 0,
}: MoneyInputProps) {
  return (
    <div className="relative">
      <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-sm text-white/45">
        R$
      </span>
      <input
        id={id}
        name={name}
        required={required}
        inputMode="decimal"
        autoComplete="off"
        placeholder={placeholder}
        className={`${error ? inputErrorClass : inputClass} pl-12`}
        value={value}
        aria-invalid={error || undefined}
        onChange={(e) => {
          const masked = maskBRLFromDigits(e.target.value);
          const numeric = parseBRLInput(masked);
          if (numeric != null && numeric < min) {
            onChange(masked, numeric);
            return;
          }
          onChange(masked, numeric);
        }}
        onBlur={() => {
          const numeric = parseBRLInput(value);
          if (numeric == null) {
            onChange("", null);
            return;
          }
          onChange(maskBRLFromDigits(String(Math.round(numeric * 100))), numeric);
        }}
      />
    </div>
  );
}
