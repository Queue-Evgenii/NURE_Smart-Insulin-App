#include <stdio.h>

double calculate_hba1c_pure(double *measurements, int length) {
    if (length <= 0 || measurements == NULL) {
        return 0.0;
    }

    double sum = 0.0;
    for (int i = 0; i < length; i++) {
        sum += measurements[i];
    }
    
    double eAG = sum / length;
    return (eAG + 2.594) / 1.594;
}