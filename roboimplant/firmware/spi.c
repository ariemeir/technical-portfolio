#include "spi.h"
#include "config.h"
#include "utils.h"

void SPI_Init(void)
{
  // ...set direction registers
  DDRB = 0xBB;    //Pin PB3 in an input, receives hall sensor input from motor encoder
  PORTB = 0xBF;
  //SCK, MOSI, SSbar out.  MISO in.

  // ...Set SPI baud rate
  SPCR = 0x51; //pretty damn slow
  SPSR = 0x00; //no doublespeed
    
  delayCycles(500);
  SPCR = 0x50;
  SPSR = 0x00;  //Increase clock speed to maximum
}

void SPI_Transmit(unsigned char data)
{
	SPDR = data;						//Load data and wait until SPI finishes sending
	while(!(SPSR & (1<<SPIF)));	
}

unsigned char SPI_Receive(void)
{
	unsigned char data;
	SPDR = 0xff;

	while(!(SPSR & (1<<SPIF)));			//Wait until data is received
	data = SPDR;

	return(data);
	
}

void SPI_Test(void)
{
  byte b=0;
  while (1)
  {
    SPI_Transmit(b);
    _delay_ms(1);
    b++;
  } 
}
