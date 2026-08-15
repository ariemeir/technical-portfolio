#include "utils.h"
#include "config.h"

void delayCycles(unsigned long cycles)
{
  for(unsigned long i=0; i<cycles; i++);
}

void blink(int n)
{
  int counter=0;
  DDRB |= 0x01;

  while(counter < n)
  {
    PORTB |= 0x01;
    _delay_ms(100);
    PORTB &= (0xFE);
    _delay_ms(100);
    counter++;
    
  }
}


void blinkOnPortA(int bit,int n)
{
  int counter=0;
  DDRA |= (1<<bit);
  while(counter < n)
  {
    PORTB |= (1<<bit);
    _delay_ms(100);
    PORTB &= ~(1<<bit);
    _delay_ms(100);
    counter++;
    
  }
}

void infiniteLoop(void)
{
  while (1) {}
}

void infiniteLoopBlinking(void)
{
  while (1) {blink(1);}
}

