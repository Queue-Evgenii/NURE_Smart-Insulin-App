#ifndef DIABETES_H
#define DIABETES_H

#include <stddef.h>

#ifdef _WIN32
    #define EXPORT __declspec(dllexport)
#else
    #define EXPORT __attribute__((visibility("default")))
#endif

#define ADAG_OFFSET 2.594
#define ADAG_DIVISOR 1.594

#ifdef __cplusplus
extern "C" {
#endif

EXPORT double calculate_hba1c_pure(const double *measurements, size_t length);
EXPORT double calculate_iob(double dose, double time_since_inj, double dia);

/* ── Glucose forecasting primitives (diploma section 2.2) ───────────────── */

EXPORT double ols_slope(const double *x, const double *y, size_t length);
EXPORT double ols_intercept(const double *x, const double *y, size_t length);
EXPORT double std_dev(const double *values, size_t length);
EXPORT double forecast_glucose(double current_glucose, double slope_per_min,
                               double horizon_min, double iob, double isf,
                               double dia_min, double carbs_on_board, double icr,
                               double carb_absorption_min);

#ifdef __cplusplus
}
#endif

#endif