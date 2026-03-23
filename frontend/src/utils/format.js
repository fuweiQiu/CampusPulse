export function formatDateTime(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("zh-TW", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatMetric(value, suffix = "") {
  if (value === null || value === undefined) {
    return "-";
  }
  return `${value}${suffix}`;
}
