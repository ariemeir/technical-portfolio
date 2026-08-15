#include<avr/io.h>   
#include<math.h>   
//#include <macros.h>   
   
#include "adc_ad7715.h"   
unsigned int ADC_buffer;   
unsigned char Ready;   
   
void Channel(unsigned char channel)   
{   
    DDRB |= 0x06;   
    channel &= 0x03;   
    if(channel == 0)   
    {   
       PORTB &= ~0x06;   
    }   
    else if(channel == 1)   
    {   
        PORTB &= ~0x02;   
        PORTB |= 0x04;   
    }   
    else if(channel == 2)   
    {   
        PORTB |= 0x02;   
        PORTB &= ~0x04;   
    }   
    else if(channel == 3)   
    {   
        PORTB |= 0x06;   
    }   
}   
void Delayx10ms(unsigned int count)   
     {  unsigned int i,j,k;   
        for(i=0;i<count;i++)   
           for (j=0;j<1950;j++)   
               for (k=0;k<10;k++)   
               ;   
     }   
void wrad7715(unsigned char cod)   
{    unsigned char i;   
     DAT_DIR_OUT;   
     for(i=0;i<8;i++)   
        { SCLK_0;   
          delay(10);   
          if(cod&0x80)   
          {   
             DAT_1;   
          }   
          else   
          {   
              DAT_0;   
          }   
          delay(10);   
          SCLK_1;   
          delay(10);   
          cod=cod<<1;   
        }   
}   
unsigned int rddata(void)   
{   unsigned char i;   
    unsigned int  dat=0;   
    DAT_DIR_IN;   
    for(i=0;i<16;i++)   
        {  dat<=1;   
           SCLK_0;   
           delay(10);   
           SCLK_1;   
           delay(10);   
           if(DAT_IN) dat|=0x0001;   
        }   
        return(dat);   
}   
   
void reset_ad7715(void)   
{   unsigned char i;   
    DAT_1;   
    DAT_DIR_OUT;   
    for(i=0;i<0x30;i++)   
       {  SCLK_0;   
          delay(10);   
          SCLK_1;   
          delay(10);   
       }   
}   
void Init_AD7715(void)    
{     
    DDRE |= 0x04;   
    DDRE &= ~0x10;   
    reset_ad7715();  
    wrad7715(0x10);  
    wrad7715(0x64);  
                    
                    
   Delayx10ms(10);   
   wrad7715(0x28);  
}   
unsigned long temp=0;   
unsigned int max=0,min=0xffff;   
unsigned char adc_time=0;   
#pragma interrupt_handler ad7715:6   
void ad7715(void)   
{   unsigned int Adc_temp=0;   
    wrad7715(0x38);   
    Adc_temp=rddata();   
    if(adc_time<10)   
     {   
       
         temp+=Adc_temp;   
         if(Adc_temp>max) max=Adc_temp;   
         if(Adc_temp<min) min=Adc_temp;   
         adc_time++;   
      }   
      else   
      {   
          AD7715_Int_STOP;   
          temp=temp-max-min;   
          ADC_buffer = temp/8;   
          Ready=1;   
          adc_time = 0;   
          temp=0;   
          max=0;   
          min=0xffff;   
      }   
       
       
}   
   
unsigned int Getadc(void)   
{    unsigned long temp;   
     unsigned int max;   
     unsigned int min;   
     unsigned char i;   
     temp=0;   
     min=0xffff;   
     max=0x0000;   
     i=0;   
     Ready=0;   
     AD7715_Int_ON;   
     while(i<10)   
     {   
        if(Ready)   
          {   Ready=0;   
              temp+=ADC_buffer;   
              if(ADC_buffer>max) max=ADC_buffer;   
              if(ADC_buffer<min) min=ADC_buffer;   
              i++;   
   
           }   
   
      }   
      AD7715_Int_STOP;   
      temp=temp-max-min;   
      temp/=8;   
      return(temp);   
