//***********************************************************
// **** MAIN routine FOR Interfacing microSD/SDHC CARD ****
//***********************************************************
//Controller: ATmega32 (Clock: 8 Mhz-internal)
//Compiler	: AVR-GCC (winAVR with AVRStudio)
//Project V.: Version - 2.4.1
//Author	: CC Dharmani, Chennai (India)
//			  www.dharmanitech.com
//Date		: 24 Apr 2011
//***********************************************************

//Link to the Post: http://www.dharmanitech.com/2009/01/sd-card-interfacing-with-atmega8-fat32.html

//#define F_CPU 8000000UL		//freq 8 MHz
#include <avr/io.h>
#include <avr/pgmspace.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include "spi_routines.h"
#include "sd_routines.h"
#include "uart.h"
#include "rtc_routines.h"
#include "i2c_routines.h"
#include "fat32.h"
#include <stdio.h>

#define UART_BAUD_RATE      57600

static int uart_putchar(char c, FILE *stream)
{
  if (c == '\n') uart_putchar('\r', stream);
  
  loop_until_bit_is_set(UCSR0A, UDRE0);
  UDR0 = c;
  
  return 0;
}

static FILE mystdout = FDEV_SETUP_STREAM(uart_putchar, NULL, _FDEV_SETUP_WRITE);
 
void init_general(void)
{
   stdout = &mystdout; //Required for printf init  
}


void port_init(void)
{
  PORTA = 0x00;
  DDRA  = 0x00;
  PORTB = 0xEF;

  //DDRB = data direction register for port B. 0 = input, 1 = output
  DDRB  = 0xBF; //MISO line input, rest output
  PORTC = 0x00;
  DDRC  = 0x00;
  PORTD = 0x00;
  DDRD  = 0xFE;
}


//call this routine to initialize all peripherals
void init_devices(void)
{
 init_general();
 cli();  //all interrupts disabled
 port_init();
 spi_init();
 twi_init();
 uart_init( UART_BAUD_SELECT(UART_BAUD_RATE,F_CPU) );
 sei();

 MCUCR = 0x00;

//Arie - comment this out for now - possibly need to find the corresponding register in 1284p
 //GICR  = 0x00;

//Arie : modified this TIMSK --> TIMSK0
 TIMSK0 = 0x00; //timer interrupt sources
 TIMSK1 = 0x00;

 //all peripherals are now initialized
}



//*************************** MAIN *******************************//
int sd_card_main(void)
{
unsigned char option, error, data, FAT32_active;
unsigned int i;
unsigned char fileName[13];

_delay_ms(100);  //delay for VCC stabilization

init_devices();

PORTD |= 0x04; //switching ON the LED (for testing purpose only)

printf("****************************************************\n");
printf("    microSD Card Testing..  \n");
printf("****************************************************\n");

cardType = 0;

for (i=0; i<10; i++)
{
  error = SD_init();
  if(!error) 
  break;
}

if(error)
{
  if(error == 1) 
    printf("SD card not detected..\n");
  if(error == 2) 
    printf("Card Initialization failed..\n");

  while(1);  //wait here forever if error in SD init 
}

SD_displayCardInfo();

SPI_HIGH_SPEED;	//SCK - 4 MHz
_delay_ms(1);   //some delay


FAT32_active = 1;
error = getBootSectorData (); //read boot sector and keep necessary data in global variables
if(error) 	
{
  printf("\n");
  printf("FAT32 not found!\n");  //FAT32 incompatible drive
  FAT32_active = 0;
}

while(1)
{
  printf("Press any key...\n");

  option = uart_getc();

  printf("> 0: Erase Blocks \n");
  printf("> 1: Write single Block          2: Read single Block\n");

  #ifndef FAT_TESTING_ONLY
    printf("\n");
    printf("> 3: Write multiple Blocks       4: Read multiple Blocks\n");
  #endif

printf("> 5: Get file list               6: Read File\n");
printf("> 7: Write File                  8: Delete File\n");
printf("> 9: Read SD Memory Capacity     a: Show Date & Time\n");
printf("> b: Update Date                 c: Update Time\n");
printf("> Select Option from (0-9/a/b/c): \n");


/*WARNING: If option 0, 1 or 3 is selected, the card data may not be detected by PC/Laptop again,
as it may disturb the FAT format. In such a case you will need to format the card again with FAT32.
This options are given for learnig the raw data transfer to & from the SD Card*/

  option = uart_getc_blocking();
  uart_putc(option);
printf("Your choice : ");
if(option >=0x35 && option <=0x39)  //options 5 to 9 disabled if FAT32 not found
{
  if(!FAT32_active) 
  {
    printf("\n");
  	printf("FAT32 options disabled!\n");
	  continue;
  } 
}


if((option >= 0x30) && (option <=0x34)) //get starting block address for options 0 to 4
{
  printf("\n");
  printf("Enter the Block number (0000-9999):\n");
  data = uart_getc_blocking(); 
  uart_putc(data);
  startBlock = (data & 0x0f) * 1000;
  data = uart_getc_blocking(); uart_putc(data);
  startBlock += (data & 0x0f) * 100;
  data = uart_getc_blocking(); uart_putc(data);
  startBlock += (data & 0x0f) * 10;
  data = uart_getc_blocking(); uart_putc(data);
  startBlock += (data & 0x0f);
  printf("\n");
}

totalBlocks = 1;

#ifndef FAT_TESTING_ONLY

if((option == 0x30) || (option == 0x33) || (option == 0x34)) //get total number of blocks for options 0, 3 or 4
{
  printf("\n");
  printf("\n");
  printf("How many blocks? (000-999):\n");
  data = uart_getc_blocking(); uart_putc(data);
  totalBlocks = (data & 0x0f) * 100;
  data = uart_getc_blocking(); uart_putc(data);
  totalBlocks += (data & 0x0f) * 10;
  data = uart_getc_blocking(); uart_putc(data);
  totalBlocks += (data & 0x0f);
  printf("\n");
}
#endif

switch (option)
{
case '0': //error = SD_erase (block, totalBlocks);
          error = SD_erase (startBlock, totalBlocks);
          printf("\n");
          if(error)
              printf("Erase failed..\n");
          else
              printf("Erased!\n");
          break;

case '1': printf("\n");
          printf(" Enter text (End with ~):\n");
          i=0;
            do
            {
                data = uart_getc_blocking();
                uart_putc(data);
                buffer[i++] = data;
                if(data == 0x0d)
                {
                    uart_putc(0x0a);
                    buffer[i++] = 0x0a;
                }
                if(i == 512) break;
            }while (data != '~');

            error = SD_writeSingleBlock (startBlock);
            printf("\n");
            printf("\n");
            if(error)
                printf("Write failed..\n");
            else
                printf("Write successful!\n");
            break;

case '2': error = SD_readSingleBlock (startBlock);
          printf("\n");
          if(error)
            printf("Read failed..\n");
          else
          {
            for(i=0;i<512;i++)
            {
                if(buffer[i] == '~') break;
                uart_putc(buffer[i]);
            }
            printf("\n");
            printf("\n");
            printf("Read successful!\n");
          }

          break;
//next two options will work only if following macro is cleared from SD_routines.h
#ifndef FAT_TESTING_ONLY

case '3': 
          error = SD_writeMultipleBlock (startBlock, totalBlocks);
          printf("\n");
          if(error)
            printf("Write failed..\n");
          else
            printf("Write successful!\n");
          break;

case '4': error = SD_readMultipleBlock (startBlock, totalBlocks);
          printf("\n");
          if(error)
            printf("Read failed..\n");
          else
            printf("Read successful!\n");
          break;
#endif

case '5': printf("\n");
  		  findFiles(GET_LIST,0);
          break;

case '6': 
case '7': 
case '8': printf("\n");
		  printf("\n");
          printf("Enter file name: \n");
          for(i=0; i<13; i++)
			fileName[i] = 0x00;   //clearing any previously stored file name
          i=0;
          while(1)
          {
            data = uart_getc_blocking();
            if(data == 0x0d) break;  //'ENTER' key pressed
			if(data == 0x08)	//'Back Space' key pressed
	 		{ 
	   			if(i != 0)
	   			{ 
	     			uart_putc(data);
					uart_putc(' '); 
	     			uart_putc(data); 
	     			i--; 
	   			} 
	   			continue;     
	 		}
			if(data <0x20 || data > 0x7e) continue;  //check for valid English text character
			uart_putc(data);
            fileName[i++] = data;
            if(i==13){printf(" file name too long..\n"); break;}
          }
          if(i>12) break;
       
	      printf("\n");
		  if(option == '6')
		     readFile( READ, fileName);
		  if(option == '7')
		  	 writeFile(fileName);
 		  if(option == '8')
		     deleteFile(fileName);
          break;

case '9': memoryStatistics();
          break;

case 'a': 
case 'A': RTC_displayDate();
		  RTC_displayTime();
		  break;
case 'b': 
case 'B': RTC_updateDate();
		  break;
case 'c': 
case 'C': RTC_updateTime();
	      break;

default: printf("\n\n");
         printf(" Invalid option!: 0x%x\n",option);
}

printf("\n");
}
return 0;
}
//********** END *********** www.dharmanitech.com *************
