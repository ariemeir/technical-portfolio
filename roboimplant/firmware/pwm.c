#include <avr/io.h>
#include <util/delay.h>
#include "pwm.h"

void initPWM(void)
{
   /*
   TCCR0 - Timer Counter Control Register (TIMER0)
   -----------------------------------------------
   BITS DESCRIPTION
   
   Timer Clock = CPU Clock (No Prescalling)
   Mode        = Fast PWM
   PWM Output  = Non Inverted

   */

   //very useful resource is the pololu avr library 
   // specifically :  ~dev/libpololu-svr/src/OrangutanMotors/OrangutanMotors.cpp

   //This is very weird since the spec claims that when wgm[2:0] =3 , we get TOP=0xFF, 
   //but it seems that i can control the duty cycle with OCR0A in this case.

   TCCR0A = (1<<WGM01)|(1<<WGM00)|(1<<COM0A1);
   //TCCR0B = (1<<CS01);
   TCCR0B = (1<<CS01)|(1<<CS00);
   

   //enable pin b1 as output
   DDRB|=(1<<PB3);
   setPWMOutput(128);
}

/******************************************************************
Sets the duty cycle of output. 

Arguments
---------
duty: Between 0 - 255

0= 0%

255= 100%

The average voltage on the output pin will be

         duty
 Vout=  ------ x 5v
         255 

This can be used to control the brightness of LED or Speed of Motor.
*********************************************************************/

void setPWMOutput(uint8_t duty)
{
   OCR0A=duty;
}


