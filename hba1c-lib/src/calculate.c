#include "../include/diabetes.h"
#include <stdio.h>

/**
 * Calculates the estimated Glycated Hemoglobin (HbA1c) percentage based on 
 * the Average Glucose (eAG) using the ADAG (A1c-Derived Average Glucose) formula.
 *
 * @param measurements Pointer to an array of blood glucose readings (mmol/L).
 * @param length       The total number of elements in the measurements array.
 * @return             The calculated HbA1c value as a percentage (%), 
 * or -1.0 if the input data is invalid (NULL pointer, empty array, 
 * or contains negative values).
 * * @note Formula: HbA1c (%) = (average_glucose + 2.594) / 1.594
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