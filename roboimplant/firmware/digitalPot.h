#ifndef DIGITAL_POTENTIOMETER_H
#define DIGITAL_POTENTIOMETER_H

#include "config.h"


void initDigitalPot(void);

//basically the speed is derived from the digital pot resistance,
//so the updateSpeed routine is effectively calling updateResistance.
void adjustSpeed(byte speed);

//adjust the resistance on the digital pot
void updateResistance(byte resistanceStep); 

//runs an infinite loop of adjusting the resistance in steps of 0x10;
void testDigitalPot(byte nsteps);

//set the speed to basically 0
void resetSpeed(void );

void slowDownToStop(void);
#endif
