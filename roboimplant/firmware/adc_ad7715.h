#ifndef ADC_AD7715_H
#define ADC_AD7715_H

void Writetoreg (int);
void Read (int,int);
void initADC_AD7715(void);
void testADC_AD7715(void);

#endif


#if 0
/* This program has read and write routines for the 68HC11 to interface to the AD7715 and the sample
program sets the various registers and then reads 1000 samples from the part. */
#include <math.h>
#include <io6811.h>
#define NUM_SAMPLES 1000 /* change the number of data samples */
#define MAX_REG_LENGTH 2 /* this says that the max length of a register is 2 bytes */
Writetoreg (int);
Read (int,char);

char *datapointer = store;
char store[NUM_SAMPLES*MAX_REG_LENGTH + 30];

void main()
{
  /* the only pin that is programmed here from the 68HC11 is the /CS and this is why the PC2 bit
  of PORTC is made as an output */
  char a;
  DDRC = 0x04; /* PC2 is an output the rest of the port bits are inputs */
  PORTC | = 0x04; /* make the /CS line high */
  Writetoreg(0x10); /* set the gain to 1, standby off and set the next operation as write to the setup register */
  Writetoreg(0x68); /* set bipolar mode, buffer off, no filter sync, confirm clock as 2.4576MHz, set output rate to 60Hz and do a self calibration */
  while(PORTC & 0x10); /* wait for /DRDY to go low */
  for(a=0;a<NUM_SAMPLES;a++);
  {
    Writetoreg(0x38); /*set the next operation for 16 bit read from the data register */
    Read(NUM_SAMPLES,2);
  }
}

Writetoreg(int byteword);
{
  int q;
  SPCR = 0x3f;
  SPCR = 0X7f; /* this sets the WiredOR mode(DWOM=1), Master mode(MSTR=1), SCK idles high(CPOL=1), /SS
  can be low always (CPHA=1), lowest clock speed(slowest speed which is master clock /32 */
  DDRD = 0x18; /* SCK, MOSI outputs */
  q = SPSR;
  q = SPDR; /* the read of the staus register and of the data register is needed to clear the interrupt which tells the user that the data transfer is complete */

  PORTC &= 0xfb; /* /CS is low */
  SPDR = byteword; /* put the byte into data register */
  while(!(SPSR & 0x80)); /* wait for /DRDY to go low */
  PORTC |= 0x4; /* /CS high */
}

Read(int amount, int reglength)
{
  int q;
  SPCR = 0x3f;
  SPCR = 0x7f; /* clear the interrupt */
  DDRD = 0x10; /* MOSI output, MISO input, SCK output */
  while(PORTC & 0x10); /* wait for /DRDY to go low */
  PORTC & 0xfb ; /* /CS is low */
  for(b=0;b<reglength;b++)
  {
    SPDR = 0;
    while(!(SPSR & 0x80)); /* wait until port ready before reading */
    *datapointer++=SPDR; /* read SPDR into store array via datapointer */
  }
  PORTC|=4; /* /CS is high */
}

#endif
