#include "samples.h"
#include "utils.h"
#include "config.h"
#include "uart.h"
#include "pwm.h"

void demoPWM(float analogVoltage)
{
   //Initialize PWM Channel 0
   initPWM();

   //float analogVoltage = 3;
   uint8_t fixed = (uint8_t)(255*(analogVoltage/5.0));
   setPWMOutput(fixed);

   while(1)
   {   
   }   
}

void demoUART(void)
{
  /*  
     *  Initialize UART library, pass baudrate and AVR cpu clock with the macro 
     *  UART_BAUD_SELECT() (normal speed mode ) or 
     *  UART_BAUD_SELECT_DOUBLE_SPEED() ( double speed mode)
     */
    uart_init( UART_BAUD_SELECT(UART_BAUD_RATE,F_CPU) );  
    //Init the second uart (has a bluesmirf bluetooth modem)
    uart1_init( UART_BAUD_SELECT(UART_BAUD_RATE,F_CPU) );  
    // now enable interrupt, since UART library is interrupt controlled
    sei();


    while (1)
    {
      unsigned int cc = uart_getc();
      if ( cc & UART_NO_DATA )
      {
      }
      else
      {
        uart_putc((unsigned char)cc);
        blink(1);
      }
    }
}
