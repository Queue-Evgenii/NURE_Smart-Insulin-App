import { toastController } from '@ionic/vue';

/** Inclusive numeric bounds for an input field. */
export interface Range {
  min: number;
  max: number;
  /** When true, only whole numbers are accepted. */
  integer?: boolean;
}

/**
 * Physiological / sane bounds shared across the whole app.
 * These mirror the backend constraints (see DtoClasses.kt) and add
 * client-side guards for the fields the backend does not validate
 * (glucose, insulin dose, carbs, glycemic index, ...).
 */
export const RANGES = {
  glucose:              { min: 1.0, max: 40.0 },
  doseUnits:            { min: 0.1, max: 100 },
  carbs:                { min: 0,   max: 1000 },
  glycemicIndex:        { min: 1,   max: 100, integer: true },
  weight:               { min: 20,  max: 300 },
  height:               { min: 50,  max: 250 },
  targetGlucoseMin:     { min: 2.0, max: 7.0 },
  targetGlucoseMax:     { min: 4.0, max: 15.0 },
  isf:                  { min: 0.5, max: 20 },
  icr:                  { min: 1,   max: 50 },
  dia:                  { min: 1,   max: 12 },
  irf:                  { min: 0.5, max: 5.0 },
  cam:                  { min: 60,  max: 240, integer: true },
  minutesUntilActivity: { min: 0,   max: 480, integer: true },
  activityDuration:     { min: 5,   max: 300, integer: true },
} as const;

/** A translatable validation error: an i18n key plus optional interpolation params. */
export interface ValidationError {
  key: string;
  params?: Record<string, unknown>;
}

export interface CheckOptions {
  /** When false, an empty value is considered valid (optional field). Defaults to true. */
  required?: boolean;
}

/**
 * Validate a single numeric value against a range.
 * Catches empty, non-numeric, NaN/Infinity, non-integer and out-of-range values.
 * Returns null when the value is valid.
 */
export function checkRange(value: unknown, range: Range, opts: CheckOptions = {}): ValidationError | null {
  const required = opts.required ?? true;

  if (value === null || value === undefined || value === '') {
    return required ? { key: 'common.validation.required' } : null;
  }

  const n = typeof value === 'number' ? value : Number(value);
  if (!Number.isFinite(n)) return { key: 'common.validation.invalid' };
  if (range.integer && !Number.isInteger(n)) return { key: 'common.validation.integer' };

  if (n < range.min || n > range.max) {
    return { key: 'common.validation.range', params: { min: range.min, max: range.max } };
  }
  return null;
}

type Field = [unknown, Range] | [unknown, Range, CheckOptions];

/** Return the first validation error from a list of [value, range, opts?] tuples, or null. */
export function firstError(fields: Field[]): ValidationError | null {
  for (const field of fields) {
    const [value, range, opts] = field;
    const err = checkRange(value, range, opts);
    if (err) return err;
  }
  return null;
}

/** Show a red toast with a validation message. */
export async function presentValidationError(message: string): Promise<void> {
  const toast = await toastController.create({ message, duration: 2500, color: 'danger' });
  await toast.present();
}
