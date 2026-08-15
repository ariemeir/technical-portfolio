#ifndef ROBOIMPLANT_PWM_H
#define ROBOIMPLANT_PWM_H

#include <avr/io.h>
#include <util/delay.h>

//initialize the pwm registers
//need to extend functionality to be able to specify the exact register
void initPWM(void);

//specify the duty cycle as a number between 0 and 0xFF
void setPWMOutput(uint8_t duty);

//setup pwm and go into inifinite loop
void demopwm(void);

#endif
