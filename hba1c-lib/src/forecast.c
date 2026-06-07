#include "../include/diabetes.h"
#include <math.h>
#include <stddef.h>

/**
 * Ordinary Least Squares slope for a set of (x, y) points.
 * Used to estimate the glucose trend (mmol/L per minute) over recent readings.
 *
 * @param x      independent variable (e.g. time in minutes, ascending)
 * @param y      dependent variable (e.g. glucose in mmol/L)
 * @param length number of points
 * @return slope of the best-fit line, or 0.0 if it cannot be computed
 *         (NULL input, fewer than 2 points, or zero variance in x).
 */
EXPORT double ols_slope(const double *x, const double *y, size_t length) {
    if (x == NULL || y == NULL || length < 2) return 0.0;

    double sum_x = 0.0, sum_y = 0.0;
    for (size_t i = 0; i < length; i++) {
        sum_x += x[i];
        sum_y += y[i];
    }
    double mean_x = sum_x / (double)length;
    double mean_y = sum_y / (double)length;

    double num = 0.0, den = 0.0;
    for (size_t i = 0; i < length; i++) {
        double dx = x[i] - mean_x;
        num += dx * (y[i] - mean_y);
        den += dx * dx;
    }
    if (den == 0.0) return 0.0;
    return num / den;
}

/**
 * OLS intercept of the best-fit line through (x, y).
 * @return intercept, or 0.0 on invalid input.
 */
EXPORT double ols_intercept(const double *x, const double *y, size_t length) {
    if (x == NULL || y == NULL || length < 2) return 0.0;

    double sum_x = 0.0, sum_y = 0.0;
    for (size_t i = 0; i < length; i++) {
        sum_x += x[i];
        sum_y += y[i];
    }
    double mean_x = sum_x / (double)length;
    double mean_y = sum_y / (double)length;

    double slope = ols_slope(x, y, length);
    return mean_y - slope * mean_x;
}

/**
 * Population standard deviation of a value series.
 * Used to derive the forecast uncertainty band: a more volatile glucose
 * series yields a wider predicted range (diploma section 2.2).
 *
 * @return standard deviation, or 0.0 on invalid input.
 */
EXPORT double std_dev(const double *values, size_t length) {
    if (values == NULL || length == 0) return 0.0;

    double sum = 0.0;
    for (size_t i = 0; i < length; i++) sum += values[i];
    double mean = sum / (double)length;

    double acc = 0.0;
    for (size_t i = 0; i < length; i++) {
        double d = values[i] - mean;
        acc += d * d;
    }
    return sqrt(acc / (double)length);
}

/**
 * Forecasts blood glucose at a future moment by composing three effects
 * (diploma section 2.2 — "Алгоритми прогнозування глікемічних станів"):
 *
 *   1. Trend extrapolation  — recent slope projected forward, damped over time
 *                             because the linear assumption weakens with horizon.
 *   2. Insulin effect       — the fraction of active insulin (IOB) that will act
 *                             within the horizon lowers glucose by that share * ISF.
 *   3. Carbohydrate effect  — carbs still being absorbed within the horizon raise
 *                             glucose, scaled by ISF/ICR (glucose rise per gram).
 *
 *   predicted = current + trend - insulin_drop + carb_rise
 *
 * @param current_glucose     latest measured glucose (mmol/L)
 * @param slope_per_min       trend from ols_slope (mmol/L per minute)
 * @param horizon_min         minutes ahead to forecast (> 0)
 * @param iob                 active insulin now (Units)
 * @param isf                 insulin sensitivity factor (mmol/L per Unit)
 * @param dia_min             duration of insulin action (minutes)
 * @param carbs_on_board      carbohydrates not yet absorbed (grams)
 * @param icr                 insulin-to-carb ratio (grams per Unit)
 * @param carb_absorption_min total carb absorption time (e.g. 120 min)
 * @return predicted glucose (mmol/L), clamped to a physiological floor of 1.0.
 */
EXPORT double forecast_glucose(double current_glucose, double slope_per_min,
                               double horizon_min, double iob, double isf,
                               double dia_min, double carbs_on_board, double icr,
                               double carb_absorption_min) {
    if (horizon_min <= 0.0) return current_glucose;

    /* 1. Damped linear trend — confidence in the slope decays with horizon. */
    double damping = exp(-horizon_min / 120.0);
    double trend = slope_per_min * horizon_min * damping;

    /* 2. Insulin effect: share of IOB consumed within the horizon. */
    double insulin_drop = 0.0;
    if (iob > 0.0 && isf > 0.0 && dia_min > 0.0) {
        double consumed_fraction = horizon_min / dia_min;
        if (consumed_fraction > 1.0) consumed_fraction = 1.0;
        insulin_drop = iob * consumed_fraction * isf;
    }

    /* 3. Carbohydrate effect: share of carbs absorbed within the horizon. */
    double carb_rise = 0.0;
    if (carbs_on_board > 0.0 && icr > 0.0 && isf > 0.0 && carb_absorption_min > 0.0) {
        double absorbed_fraction = horizon_min / carb_absorption_min;
        if (absorbed_fraction > 1.0) absorbed_fraction = 1.0;
        double absorbed_carbs = carbs_on_board * absorbed_fraction;
        carb_rise = absorbed_carbs * (isf / icr);
    }

    double predicted = current_glucose + trend - insulin_drop + carb_rise;
    return (predicted < 1.0) ? 1.0 : predicted;
}
