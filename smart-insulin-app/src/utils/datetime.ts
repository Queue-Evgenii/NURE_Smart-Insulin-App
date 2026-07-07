/**
 * Helpers for wiring `<ion-datetime>` (which works with local, timezone-less ISO
 * strings) to the backend (which expects UTC ISO-8601 instants).
 */

/** Convert a Date to a local `YYYY-MM-DDTHH:mm` string that `ion-datetime` understands. */
export function toLocalISO(date: Date): string {
  const tzOffsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - tzOffsetMs).toISOString().slice(0, 16);
}

/** Current local time as an `ion-datetime` compatible string. */
export function nowLocalISO(): string {
  return toLocalISO(new Date());
}

/** Convert a stored UTC ISO instant to the local value expected by `ion-datetime`. */
export function apiToLocalISO(apiISO: string): string {
  return toLocalISO(new Date(apiISO));
}

/** Convert an `ion-datetime` local value to a UTC ISO string for the API. */
export function localToApiISO(local: string): string {
  return new Date(local).toISOString();
}
