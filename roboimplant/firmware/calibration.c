#include "calibration.h"
#include "digitalPot.h"
#include "printFloat.h"
#include "adc.h"
#include <avr/eeprom.h>

//EEPROM Functions declarations:

/*uint8_t eeprom_read_byte (const uint8_t *addr)
void eeprom_write_byte (uint8_t *addr, uint8_t value)
uint16_t eeprom_read_word (const uint16_t *addr)
void eeprom_write_word (uint16_t *addr, uint16_t value)
void eeprom_read_block (void *pointer_ram, const void *pointer_eeprom, size_t n)
void eeprom_write_block (void *pointer_eeprom, const void *pointer_ram, size_t n) 
*/



//the data saved in eeprom as scaled floats (cast as integers)
#define CALIBRATION_SCALING_FACTOR  1000.0

//this maps the calibration data into the eeprom memory space
#define LOW_SPEED_CURRENT_HIGH_BYTE     1
#define LOW_SPEED_CURRENT_LOW_BYTE      2
#define HIGH_SPEED_CURRENT_HIGH_BYTE    3
#define HIGH_SPEED_CURRENT_LOW_BYTE     4 

#define N_SAMPLES_FOR_AVERAGING         4


float getAveragedCurrentValue(int nsamples);


void initCalibration(void)
{
}

void runCalibration(CalibrationParams* pcalibParams)
{
  byte speed;
  speed=LOW_SPEED;
  adjustSpeed(speed);
  pcalibParams->lowSpeedCurrent = getAveragedCurrentValue(N_SAMPLES_FOR_AVERAGING);
  
  speed=HIGH_SPEED;
  adjustSpeed(speed);
  pcalibParams->highSpeedCurrent = getAveragedCurrentValue(N_SAMPLES_FOR_AVERAGING);
}

void saveCalibrationData(CalibrationParams* pcalibParams)
{
  unsigned int scaledCurrent;
  byte highpart,lowpart;


  //save low speed data
  scaledCurrent = (unsigned int)(pcalibParams->lowSpeedCurrent*CALIBRATION_SCALING_FACTOR);
  highpart=scaledCurrent>>8;

  //Save values for low speed calibration to eeprom 
  eeprom_write_byte ((byte*)LOW_SPEED_CURRENT_HIGH_BYTE, (byte)highpart);         
  lowpart = scaledCurrent&0xFF;
  eeprom_write_byte ((byte*)LOW_SPEED_CURRENT_LOW_BYTE, (byte)lowpart);

  //save high speed data
 
  scaledCurrent = (unsigned int)(pcalibParams->highSpeedCurrent*CALIBRATION_SCALING_FACTOR);
  highpart=scaledCurrent>>8;

  //Save values for low speed calibration to eeprom 
  eeprom_write_byte ((byte*)HIGH_SPEED_CURRENT_HIGH_BYTE, (byte)highpart);         
  lowpart = scaledCurrent&0xFF;
  eeprom_write_byte ((byte*)HIGH_SPEED_CURRENT_LOW_BYTE, (byte)lowpart);
}

void loadCalibrationData(CalibrationParams* pcalibParams)
{
  
  unsigned int scaledCurrent;
  byte highpart,lowpart;

  //read low speed data
  highpart = eeprom_read_byte ((byte*)LOW_SPEED_CURRENT_HIGH_BYTE);         
  lowpart = eeprom_read_byte ((byte*)LOW_SPEED_CURRENT_LOW_BYTE);
  scaledCurrent = (highpart<<8)|lowpart;
  pcalibParams->lowSpeedCurrent = (float)scaledCurrent/CALIBRATION_SCALING_FACTOR;

  //read high speed data
  highpart = eeprom_read_byte ((byte*)HIGH_SPEED_CURRENT_HIGH_BYTE);         
  lowpart = eeprom_read_byte ((byte*)HIGH_SPEED_CURRENT_LOW_BYTE);
  scaledCurrent = (highpart<<8)|lowpart;
  pcalibParams->highSpeedCurrent = (float)scaledCurrent/CALIBRATION_SCALING_FACTOR;
}

float getAveragedCurrentValue(int nsamples)      
{
  float totalCurrent=0.0,current;


  _delay_ms(20);            //Wait for current to settle, motor to be stable
  for (int i=0;i<nsamples;i++)
  {
     if (measureCurrent(&current))
     {
       
       fprintf(stderr,"Current from sample #%d : ",i);
       printFloat(current,3,stderr);
       NEWLINE_STDERR;
       totalCurrent += current;
       _delay_ms(1);
    }
  }  
    
  totalCurrent = totalCurrent/nsamples;
  fprintf(stderr,"Averaged current from %d samples : ",nsamples);
  printFloat(totalCurrent,3,stderr);
  NEWLINE_STDERR; 

  return totalCurrent;
}

