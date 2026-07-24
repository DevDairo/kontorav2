export function normalizeMoneyInput(value: string) {
  const compactValue = value.replace(/[^\d.,]/g, "");
  const commaIndex = compactValue.lastIndexOf(",");
  const dotIndex = compactValue.lastIndexOf(".");
  const decimalIndex = commaIndex >= 0 ? commaIndex : dotIndex;

  if (decimalIndex < 0) {
    return compactValue.replace(/[^\d]/g, "");
  }

  const decimalDigits = compactValue.slice(decimalIndex + 1).replace(/[^\d]/g, "");
  const usesDecimalSeparator = commaIndex >= 0 || decimalDigits.length <= 2;

  if (!usesDecimalSeparator) {
    return compactValue.replace(/[^\d]/g, "");
  }

  const integerDigits = compactValue.slice(0, decimalIndex).replace(/[^\d]/g, "");
  return `${integerDigits || "0"}.${decimalDigits.slice(0, 2)}`;
}
