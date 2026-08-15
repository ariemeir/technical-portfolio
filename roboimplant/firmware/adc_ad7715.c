#include "config.h"
#include "uart.h"
#include "adc_ad7715.h"
#include <math.h>
#include <avr/io.h>



#define NUM_SAMPLES 10 /* change the number of data samples */
#define MAX_REG_LENGTH 2 /* this says that the max length of a register is 2 bytes */

char store[NUM_SAMPLES*MAX_REG_LENGTH + 30];
char *datapointer = store;

void initADC_AD7715(void)
{
  fprintf(stderr,"Initializing AD7715\n");
  //define PB1 as the CS for adc ad7715 (output bit)
  sbi(DDRB,1);
  //define PB4 as the /DDRY bit from the adc (input bit)
  cbi(DDRB,4);
 
  //disable the ADC  : set its /cs to high 
  sbi(PORTB,1); 

  Writetoreg(0x10); /* set the gain to 1, standby off and set the next operation as write to the setup register */
  Writetoreg(0x68); /* set bipolar mode, buffer off, no filter sync, confirm clock as 2.4576MHz, set output rate to 60Hz and do a self calibration */
  
  //Arie : trying to crank up that level of abstraction
  //SPI_Transmit(0x10);
  //SPI_Transmit(0x68);
  fprintf(stderr,"Waiting for DRDY to go low\n");
  while(PORTB & PORTB4); /* wait for /DRDY to go low */

  fprintf(stderr,"Finished init.\n");
}

void testADC_AD7715(void)
{
  for(byte i=0;i<NUM_SAMPLES;i++)
  {
    Writetoreg(0x38); /*set the next operation for 16 bit read from the data register */
    Read(NUM_SAMPLES,2);
    int value = store[2*i]<<8 | store[2*i+1];
    fprintf(stderr,"value : %d\n",value);
  }
 
}

void Writetoreg(int byteword)
{
  int q;
  SPCR = 0x3f;
  SPCR = 0X7f; /* this sets the WiredOR mode(DWOM=1), Master mode(MSTR=1), SCK idles high(CPOL=1), /SS
  can be low always (CPHA=1), lowest clock speed(slowest speed which is master clock /32 */
  //DDRD = 0x18; /* SCK, MOSI outputs */
  q = SPSR;
  q = SPDR; /* the read of the staus register and of the data register is needed to clear the interrupt which tells the user that the data transfer is complete */

  cbi(PORTB,1); /* /CS is low */
  SPDR = byteword; /* put the byte into data register */
  while(!(SPSR & 0x80)); /* wait for /DRDY to go low */
  sbi(PORTB,1); /* /CS high */
}

void Read(int amount, int reglength)
{
  //int q;
  SPCR = 0x3f;
  SPCR = 0x7f; /* clear the interrupt */
  //DDRD = 0x10; /* MOSI output, MISO input, SCK output */
  while(PORTB & 0x10); /* wait for /DRDY to go low */
  cbi(PORTB,1); /* /CS is low */
  for(int i=0;i<reglength;i++)
  {
    SPDR = 0;
    while(!(SPSR & 0x80)); /* wait until port ready before reading */
    *datapointer++=SPDR; /* read SPDR into store array via datapointer */
  }
  sbi(PORTB,1); // set /cs to high
}




