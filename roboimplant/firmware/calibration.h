#ifndef CALIBRATION_H
#define CALIBRATION_H

#include "config.h"


void initCalibration(void);

void runCalibration(CalibrationParams* pcalibParams);
void saveCalibrationData(CalibrationParams* pcalibParams);
void loadCalibrationData(CalibrationParams* pcalibParams);


#endif
