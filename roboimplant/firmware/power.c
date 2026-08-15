#include "config.h"
#include "power.h"


byte isMotorOn=FALSE;

void initPower(void)
{
  sbi(DDRD, DDD4);
  turnMotorOff();
  //turnMotorOn();
}

void turnMotorOn(void)
{
  sbi(PORTA,PORTA5);
  isMotorOn=TRUE;
}


void turnMotorOff(void)
{
  cbi(PORTA,PORTA5);
  isMotorOn=FALSE;
}

void toggleMotorPower(void)
{
  if (isMotorOn)
    turnMotorOff();
  else
    turnMotorOn();
}

bool isMotorRunning()
{
  return isMotorOn;
}
