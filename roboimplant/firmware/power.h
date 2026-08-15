/* power.h - this file contains the routines that deal with the power aspects of the circuit board */

#ifndef POWER_CONTROL_H
#define POWER_CONTROL_H

void initPower(void);

void turnMotorOn(void);
void turnMotorOff(void);
void toggleMotorPower(void);
bool isMotorRunning(void);
#endif
