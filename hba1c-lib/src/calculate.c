#include <stdio.h>
#include <stddef.h>
#include <math.h>

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

/**
 * Calculates HbA1c based on estimated Average Glucose (eAG) in mmol/L.
 * * @param measurements - Pointer to an array of glucose readings (mmol/L).
 * @param length - Number of elements in the array.
 * @return HbA1c percentage (%), or -1.0 if input data is invalid.
 */
EXPORT double calculate_hba1c_pure(const double *measurements, size_t length) {
    if (measurements == NULL || length == 0) {
        return -1.0; 
    }

    double sum = 0.0;
    for (size_t i = 0; i < length; i++) {
        if (measurements[i] < 0) return -1.0; 
        sum += measurements[i];
    }
    
    double eAG = sum / (double)length;

    // HbA1c (%) = (eAG + 2.594) / 1.594
    return (eAG + ADAG_OFFSET) / ADAG_DIVISOR;
}

#ifdef __cplusplus
}
#endif