#ifndef ROBOIMPLANT_ADC_H
#define ROBOIMPLANT_ADC_H

#include "config.h"

#define ADC_REFERENCE_VOLTAGE 5.0

//it is possible to sample more often at the price of precision bits (not 24 as the top limit allows but less)
//i've noticed that if this number goes below 300, i get weird numbers as i sample
//how often do we sample the adc (in msec units)
#define ADC_SAMPLE_INTERVAL 170

void initADC(void);
bool measureVoltage(float* pvoltage);
//return true if the value in *pcurrent represents the current, false otherwise (error)
bool measureCurrent(float* pcurrent);


void testADCSingleIteration(void);
void testADC(int adcIndex);

#endif
