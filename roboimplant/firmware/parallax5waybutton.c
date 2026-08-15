#include "parallax5waybutton.h"
//#include "serialLCD.h"

//how many read do we do before we decide it is indeed high
#define N_RETRIES 3
//how many of them have actually to be high.
#define N_PASS 1

void clearLCD(void)
{
}

void initParallax5WayButton(void)
{
  //set the button pins as inputs
  DDRA=0x1F;
  //configure the internal pulls up on
  PORTA|=0x1F;
}

byte numButtons=5;
byte buttons[] = {PUSH_UP,PUSH_DOWN,PUSH_LEFT,PUSH_RIGHT,PUSH_SELECT};

byte leftCnt=0;
byte rightCnt=0;
byte upCnt=0;
byte downCnt=0;
byte selectCnt=0;

bool isPinLow(volatile uint8_t *pin,uint8_t pinNumber)
{
  byte pressed=0;
  for (byte i=0;i<N_RETRIES;i++)
  {  
    if (((*pin) & (1<<pinNumber))==0)
      pressed++;
  }
  return (pressed>=N_PASS);
  
}

bool isLeftPressed(void)
{
  return FALSE;//isPinLow(&PIND,PIND7);
}

bool isRightPressed(void)
{
  return FALSE;//isPinLow(&PINC,PINC1);  
}

bool isUpPressed(void)
{
  return isPinLow(&PINA,PA1);
}

bool isDownPressed(void)
{
  return isPinLow(&PINA,PA2);  
}

bool isSelectPressed(void)
{
  return isPinLow(&PINA,PA0);
}

byte buttonsPressed(void)
{
  byte buttons=0x0;
  if (isLeftPressed())
    buttons |=PUSH_LEFT;
  if (isRightPressed())
    buttons |=PUSH_RIGHT;
  if (isUpPressed())
    buttons |=PUSH_UP;
  if (isDownPressed())
    buttons |=PUSH_DOWN;
  if (isSelectPressed())
    buttons |=PUSH_SELECT;
  return buttons;
}

void waitUntilReleased(byte button)
{
  if (button==PUSH_LEFT)
  {
    while (isLeftPressed()) {}
  }
  if (button==PUSH_RIGHT)
  {
    while (isRightPressed()) {}
  }
  if (button==PUSH_UP)
  {
    while (isUpPressed()) {}
  }
  if (button==PUSH_DOWN)
  {
    while (isDownPressed()) {}
  }
  if (button==PUSH_SELECT)
  {
    while (isSelectPressed()) {}
  }
}


void waitUnilPressed(byte button)
{
  if (button==PUSH_LEFT)
  {
    while (!isLeftPressed()) {}
  }
  if (button==PUSH_RIGHT)
  {
    while (!isRightPressed()) {}
  }
  if (button==PUSH_UP)
  {
    while (!isUpPressed()) {}
  }
  if (button==PUSH_DOWN)
  {
    while (!isDownPressed()) {}
  }
  if (button==PUSH_SELECT)
  {
    while (!isSelectPressed()) {}
  }
}

void testParallax5WayButton(FILE* outstream)
{
  initParallax5WayButton();
  while (1)
  {
   //clearLCD();
   //fprintf(outstream,"C:0x%x, D:0x%x",pinc,pind);
   //_delay_ms(10);
   //continue; 
   if (isLeftPressed())
    {
      clearLCD();
      fprintf(outstream,"Left :%d",leftCnt++);
    }
    if (isRightPressed())
    {
      clearLCD();
       fprintf(outstream,"Right :%d",rightCnt++);
    } 
    if (isUpPressed())
    {
      clearLCD();
      fprintf(outstream,"Up :%d",upCnt++);
    } 
    if (isDownPressed())
    {
      clearLCD();
      fprintf(outstream,"Down :%d",downCnt++);
    }
    if (isSelectPressed())
    {
      clearLCD();
      fprintf(outstream,"Select : %d",selectCnt++);
    } 
  }
}


void waitUntilAllButtonsReleased(void)
{
  for (byte i=0;i<numButtons;i++)
    waitUntilReleased(i);
}
