#include "digitalPot.h"
#include "config.h"
#include <stdio.h>
#include "spi.h"
#include "pwm.h"

#define DIGITAL_POT_PIN_SETTING DDA4
#define DIGITAL_POT_PIN PORTA4

void initDigitalPot(void)
{
  //pin PA2 is the /cs bit of the digital pot
  //ds1267 uses the /rst notation, operating when /rst is high
  sbi(DDRA,DIGITAL_POT_PIN_SETTING);
  //disable communication with the pot.
  cbi(PORTA,DIGITAL_POT_PIN);
  resetSpeed(); 
}

void updateResistance(byte resistanceStep)    
{ 
  fprintf(stderr,"Adjusting resistance to 0x%x\n",resistanceStep);
  //enable communication with the pot.
  sbi(PORTA,DIGITAL_POT_PIN);
  
  fprintf(stderr,"Adjusting resistance of POT#1 to 0x%x\n",resistanceStep);
  //stack select : 0 (see ds1267 datasheet)
  SPI_Transmit(0x0);
  //resistance of pot#1
  SPI_Transmit(resistanceStep);
  //resistance of pot#0 (not really used);
  SPI_Transmit(resistanceStep);
 
  //disable communication with the pot  
  cbi(PORTA,DIGITAL_POT_PIN); 
}


void adjustSpeed(byte speed)
{
//  updateResistance(speed);
    setPWMOutput(speed);
}

void testDigitalPot(byte nSteps)
{
   byte speed=0x0;
   byte stepSize = 0x100/nSteps;
   while (1) 
   {
      adjustSpeed(speed); 
     _delay_ms(50);
     speed = speed + stepSize ;
//   blink(1);
   }
}

void resetSpeed(void)
{
  adjustSpeed(HALTING_SPEED_VALUE); 
}

void setSpeedSlow(void)
{
}

void setSpeedMedium(void)
{
}

void setSpeedFast(void)
{
}


void slowDownToStop()
{
  resetSpeed();
}
