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

#ifdef __cplusplus
}
#endif

#endif