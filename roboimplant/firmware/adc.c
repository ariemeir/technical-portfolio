#include "config.h"
#include "adc.h"
#include "spi.h"
#include "printFloat.h"
#include "utils.h"
#include "stdiodefs.h"


//the vcc level for the conditioning chain is used to derive divided voltages
#define VCC_CONDITIONING_CHAIN  14.94

//the voltage division factor in the conditioning chain
//it is now the same for both voltage/current sensing parts, but can be extended easily
float divisionFactor = (33000.0+6800.0)/6800.0;
  
//the reference voltage in the instrumentation amplifier.
// Typically Vcc/2 since we are trying to center our signal around 0
float baseLineInstrAmp = VCC_CONDITIONING_CHAIN/2.0; 

void initADC(void)
{
  //enable the cs bit of adc as an output bit
  sbi(DDRB, DDB1);
  sbi(DDRA, DDA4);
  sbi(DDRD, DDD7);

  fprintf(stderr,"ADC initialized\n");
}



bool measureVoltage(float* pvoltage)    //Measures voltage across motor, motor voltage related to speed
{
  //printf("Measuring voltage\n");
  //i am using ltc2400, 4 bytes of output
  //int status=0,msb=0,medium=0,lsb=0;
  unsigned long int total;
  long int ltw=0;
  byte b0=0,b1,b2,b3;
  total=0;
  byte sig=0;
  float Vadc=0.0;
  float v_ref = ADC_REFERENCE_VOLTAGE;

  //Enable ADC for voltage sense (speed sense)
  cbi(PORTB,PORTB1);   
  delayCycles(5000);
  
  b0 = SPI_Receive();
  if ((b0 & 0x20) ==0) 
    sig=1;  // is input negative ?
  b0 &=0x1F;                   // discard bit 25..31
  ltw |= b0; 
  ltw <<= 8;
  //fprintf(stderr,"b0[0] = 0x%x ",b0);  

  b1 = b0 = SPI_Receive();
  ltw |= b0; 
  ltw <<= 8;
  //fprintf(stderr,"b0[1] = 0x%x ",b0);  
 
  b2 = b0 = SPI_Receive();
  ltw |= b0; 
  ltw <<= 8;
  //fprintf(stderr,"b0[2] = 0x%x ",b0);  
 
  b3 = b0 = SPI_Receive();
  ltw |= b0; 
  //fprintf(stderr,"b0[3] = 0x%x\n",b0);  
  
  delayCycles(5000);

  //Disable ADC for voltage sense (speed sense)
  sbi(PORTB,PORTB1);
  if (sig) 
    ltw |= 0xf0000000;    // if input negative insert sign bit
  ltw=ltw/16;                    // scale result down , last 4 bits have no information
  Vadc = ltw * v_ref / 16777216; // max scale
  
  //bug alert - once in a while, the adc returns 10v on its input. this is clearly an error. 
  // needs to be debugged, but for now just return false meaning that this is an invalid value  
  if (fabs(Vadc-10.0)<0.01)
  {
    //fprintf(stderr,"Error reading voltage value from adc : [b0 b1 b2 b3] = [0x%x 0x%x 0x%x 0x%x]\n",b0,b1,b2,b3);
    //fprintf(stderr,"-v");
    return FALSE;
  }


  //effectively we measure the halved voltage on between motor+ and motor- so this is why we have 2.0 in the front 
  //the second 2.0 is produced by a fixed gain amplifier in the conditioning chain.
  //note that filtering effects are not considered and will lead to innaccuracies
  float motorVoltage = 2.0*( Vadc / 2.0 * divisionFactor - baseLineInstrAmp);
  
   //fprintf(stdout," Vadc(voltage) : ");
  //printFloat(   Vadc,6,stdout ); 
  //NEWLINE_STDOUT;
  //fprintf(stderr,"Division factor : ");
  //printFloat(  divisionFactor,6,stderr);
  //NEWLINE_STDERR;
  //fprintf(stderr,"Half way there : ");
  //printFloat(   Vadc/2.0*divisionFactor, 6,stderr);
  //NEWLINE_STDERR; 

  //fprintf(stderr,"\n Motor voltage : ");
  //printFloat(voltageOnMotor,6,stderr);
  //fprintf(stderr,"\n");  
  
  //motorVoltage=Vadc; //for debugging purposes - return the voltage on the adc
  *pvoltage = motorVoltage;
  return TRUE;
}

bool measureCurrent(float* pCurrent )
{
 
  printf("Measuring current\n");
  //i am using ltc2400, 4 bytes of output
  //int status=0,msb=0,medium=0,lsb=0;
  //volatile unsigned long int total=0;
  volatile long int ltw=0;
  volatile byte b0=0,b1,b2,b3;
  volatile byte sig=0;
  volatile float Vadc=0.0;
  float v_ref = ADC_REFERENCE_VOLTAGE;

  //define the /cs bit of the adc as output 
  //sbi(DDRB,DDA1);  
  //Enable ADC for current sense (torque sense)
  cbi(PORTA,PORTA4);

  delayCycles(5000);
  
  b0 = SPI_Receive();
  if ((b0 & 0x20) ==0) 
    sig=1;  // is input negative ?
  b0 &=0x1F;                   // discard bit 25..31
  ltw |= b0; 
  ltw <<= 8;
  //fprintf(stderr,"b0[0] = 0x%x ",b0);  
  

  b1 = b0 = SPI_Receive();
  ltw |= b0; 
  ltw <<= 8;
  //fprintf(stderr,"b0[1] = 0x%x ",b0);  
 
  b2 = b0 = SPI_Receive();
  ltw |= b0; 
  ltw <<= 8;
  //fprintf(stderr,"b0[2] = 0x%x ",b0);  
 
  b3 = b0 = SPI_Receive();
  ltw |= b0; 
  //fprintf(stderr,"b0[3] = 0x%x\n ",b0);  
  
  delayCycles(5000);
  
  //Disable ADC for voltage sense (speed sense)
  sbi(PORTA,PORTA4);
  if (sig) 
    ltw |= 0xf0000000;    // if input negative insert sign bit
  ltw=ltw/16;                    // scale result down , last 4 bits have no information
  Vadc = ltw * v_ref / 16777216; // max scale

  //bug alert - once in a while, the adc returns 10v on its input. this is clearly an error. 
  // needs to be debugged, but for now just return false meaning that this is an invalid value  

  //fprintf(stderr,"About to sample....\n");

  if (fabs(Vadc-10.0)<0.01)
  {
    //fprintf(stderr,"Error reading current value from adc : [b0 b1 b2 b3] = [0x%x 0x%x 0x%x 0x%x]\n",b0,b1,b2,b3);
    //fprintf(stderr,"-c");
    return FALSE;
  }

  //fprintf(stderr,"The value read from the current adc :");
  //printFloat(Vadc,6,stderr); 
  
  //how much gain does the initial instrumentation amp give us
  volatile float Rg = 2400; //ohms
  volatile float gainFactorInstrAmp = (49400.0/Rg +1.0);
  //effectively we measure the voltage on a precision resistor so what is this voltage:
  volatile float voltageOnSenseResistor = (Vadc / 2.0 * divisionFactor - baseLineInstrAmp) / gainFactorInstrAmp;
  volatile float precisionResistorValue = 1.3;
   
  volatile float current = voltageOnSenseResistor/precisionResistorValue;
 
  bool dumpRawCurrent = 0;
  // this delay was necessary, otherwise i got bogus values, but i incorporated it into the ADC_SAMPLE_INTERVAL _delay_ms(100);
  //troubleshooting information
  if (00 && (dumpRawCurrent || fabs(current*1000)>400)) //we shouldn't have currents > 400ma. if we do - dump the info
  {
    fprintf(stderr," Vadc(current) =");
    printFloat(Vadc,3,stderr);
    //fprintf(stderr, ", Gain factor= ");
    //printFloat(gainFactorInstrAmp,3,stderr);
    //fprintf(stderr, ", DivisionFactor= ");
    //printFloat(divisionFactor,3,stderr);
    //fprintf(stderr, ", BaseLine= ");
    //printFloat(baseLineInstrAmp,3,stderr);
    //fprintf(stderr,",sense resistor voltage= ");
    //printFloat(voltageOnSenseResistor,6,stderr);
    //fprintf(stderr,", current= ");
    //printFloat(current,6,stderr);
    NEWLINE_STDERR;
  }
  //current=Vadc; //for debugging purposes - return the voltage on the adc
  
  //the division factor 10 is configured using the escon studio
  *pCurrent = Vadc/10.0;
  return TRUE;  
}


void testADC(int adcIndex)
{
  int interval=90;//ADC_SAMPLE_INTERVAL;
  while (1)
  {
    testADCSingleIteration();
    _delay_ms(interval);
  }
}

void testADCSingleIteration(void)
{
    float value;
    bool success=TRUE;
    success = measureCurrent(&value);

    if (success)
    {
      fprintf(stderr,"Read current value ok, value is:");
      printFloat(value,6,stderr);
      NEWLINE_STDERR;
    }
    else 
    {
      fprintf(stderr,"Could not read current value  \n");
    }

    success = measureVoltage(&value);

    if (success)
    {
      fprintf(stderr,"Read voltage value ok, value is:");
      printFloat(value,6,stderr);
      NEWLINE_STDERR;
    }
    else 
    {
      fprintf(stderr,"Could not read voltage value \n");
    }

}




















//This is an incomplete attempt to configure the internal ADC. I've left it unfinished since i prefer to 
//focus on other parts of the system and wait till my ltc2400 sample arrives
#if 0  

void startConversion(int channel, bool use_internal_reference)
{
 ADCSRA = 0x87;    // bit 7 set: ADC enabled
            // bit 6 clear: don't start conversion
            // bit 5 clear: disable autotrigger
            // bit 4: ADC interrupt flag
            // bit 3 clear: disable ADC interrupt
            // bits 0-2 set: ADC clock prescaler is 128
            //  128 prescaler required for 10-bit resolution when FCPU = 20 MHz
     
  // NOTE: it is important to make changes to a temporary variable and then set the ADMUX
  // register in a single atomic operation rather than incrementally changing bits of ADMUX.
  // Specifically, setting the ADC channel by first clearing the channel bits of ADMUX and
  // then setting the ones corresponding to the desired channel briefly connects the ADC
  // to channel 0, which can affect the ADC charge capacitor.  For example, if you have a
  // high output impedance voltage on channel 1 and a low output impedance voltage on channel
  // 0, the voltage on channel 0 be briefly applied to the ADC capacitor before every conversion,
  // which could prevent the capacitor from settling to the voltage on channel 1, even over
  // many reads.
  unsigned char tempADMUX = ADMUX;

  tempADMUX |= 1 << 6;
  if(use_internal_reference)  // Note: internal reference should NOT be used on devices
  {             //  where AREF is connected to an external voltage!
    // use the internal voltage reference
    tempADMUX |= 1 << 7;    // 1.1 V on ATmega48/168/328; 2.56 V on ATmega324/644/1284
  }
  else
  {
    // use AVCC as a reference
    tempADMUX &= ~(1 << 7); 
  }

  tempADMUX &= ~0x1F;    // clear channel selection bits of ADMUX
  tempADMUX |= channel;    // we only get this far if channel is less than 32
  ADMUX = tempADMUX;
  ADCSRA |= 1 << ADSC; // start the conversion

}

static inline unsigned char isConverting()
{
    return (ADCSRA >> ADSC) & 1;
}


void initInternalADC(void)
{ 
  // 10_bit mode (see OrangutanAnalog in pololu-lib for example)
  ADMUX &= ~(1 << ADLAR); // right-adjust result (ADC has result)
}

int readInternalADC(int channel)
{
   startConversion(channel,TRUE);
   while (isConverting());
   int adcResult = ADC;
   int millivolt_calibration = 5000;
   unsigned long temp = adcResult * (unsigned long)millivolt_calibration;
   float value = (temp + 511.0) / 1023;
   

}
#endif
