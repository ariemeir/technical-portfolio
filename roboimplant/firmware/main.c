/*
 * GccApplication1.c
 *
 * Created: 8/18/2012 12:49:39 PM
 *  Author: arie
 */ 

 
//#define F_CPU 20000000

#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>
#include "utils.h"
#include "stdiodefs.h"
#include "uart.h"
#include "printFloat.h"
#include "pwm.h"
#include "adc.h"
#include "adc_ad7715.h"
#include "spi.h"
#include "digitalPot.h"
#include "config.h"
#include "messageDispatcher.h"
#include "sdcard/sd_routines.h"
#include "power.h"
#include "parallax5waybutton.h"
#include <stdlib.h>
#include "calibration.h"

PhysicalParamsBlock phyParams;
CommandParams cmdParams;
DebugParams dbgParams;
CalibrationParams calibParams;
ProcedureParams procedureParams;

//this is a quick hack to support the parallax 5 way joystick
#define MOTOR_STOPPED   0
#define MOTOR_CW        1
#define MOTOR_CCW       2
int motorMode=0;

 
//redirect the standard output stream to BT uart and and error stream to rs232 uart
static FILE mystderr = FDEV_SETUP_STREAM(uart_putchar, NULL, _FDEV_SETUP_WRITE);
#ifdef USE_RS232_UART
  static FILE mystdout = FDEV_SETUP_STREAM(uart_putchar, NULL, _FDEV_SETUP_WRITE);
#else
  static FILE mystdout = FDEV_SETUP_STREAM(uart1_putchar, NULL, _FDEV_SETUP_WRITE);
#endif

#ifdef USE_RS232_UART
  #define COMMAND_INPUT_FUNCTION  uart_getc
#else
  #define COMMAND_INPUT_FUNCTION  uart1_getc
#endif

void setDefaultParamValues()
{
  //physical parameters initial values
  phyParams.motorSpeed = HALTING_SPEED_VALUE; //half way between 0 and 255
  phyParams.torqueConstant = TORQUE_CONSTANT;
  phyParams.speedConstant = SPEED_CONSTANT;

  //Debug related parameters
  dbgParams.duplicateInputToStdErr = TRUE;
  dbgParams.sendPeriodicUpdates = FALSE;
  dbgParams.updateFrequencyMsec = DATA_UPDATE_INTERVAL;  
  
  resetCounters();
}

void initTimers(void)
{
 
    // initialize Timer1
    cli();          // disable global interrupts
    TCCR2A = 0;     // set entire TCCR2A register to 0
    TCCR2B = 0;     // same for TCCR2B

    //Nominal period = 1/16000000
    //Prescaling x N makes the period N/16000000
    //By providing the value of OCR2A, we get a period between interrupts to OCR2A*N/16000000 = (250*64/16000000) = 1.024ms.
    //If we want interrupt every second, we would get 
    // set compare match register to desired timer count:
    OCR2A = 250; 
    // turn on CTC mode:
    TCCR2A |= (1 << WGM21);
    //  64 prescaler: (see page 155 in the full manual of 1284p)
    TCCR2B |= (1 << CS22);//|(1 << CS20);//|(1 << CS20);
    // enable timer compare interrupt:
    TIMSK2 |= (1 << OCIE2A);
    sei();          // enable global interrupts
}


void enableExternalInterrupts(void)
{
  sei();

  //enable interrupts
  // details for this can be seen here : https://coefs.uncc.edu/sjkuyath/files/2012/04/Topic_16.pdf
  EICRA=EICRA|0b00110000;     //Turn on spin counter, enables INT2 trigger  , interrupt on high for a rising edge
  EIMSK=EIMSK|0b00000100;     //Enable interrupt 0x2

  cbi(DDRB,DDB2); 
  cbi(PORTB,PORTB2);
}

void initGeneralSettings(void)
{
  //do this first since there are some critical stuff there
  //initPower();

	stdout = &mystdout; //Required for printf init	
	stderr = &mystderr; //Required for printf init	

  memset(&phyParams,0,sizeof(PhysicalParamsBlock));
  memset(&cmdParams,0,sizeof(CommandParams));
  memset(&dbgParams,0,sizeof(DebugParams));
  memset(&calibParams,0,sizeof(CalibrationParams));
  memset(&procedureParams,0,sizeof(ProcedureParams));

  setDefaultParamValues();

  initTimers();

  SPI_Init();  

  enableExternalInterrupts();

  //set the PA5 and PA6 pins as digital outputs that feed into the escon 36/2 dc controller
  //enable the motor controller
  sbi(DDRA,PORTA5);
  //direction : 1 = cw , 0 =ccw
  sbi(DDRA,PORTA6);
}


void initUart(void)
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
	  printf("UART inited\n");
	
}

void initSDCard(void)
{
#ifdef USE_SDCARD //only if we compile with this option (save precious flash space)
  int error=0;
  for (int i=0; i<3; i++)
  {
    fprintf(stderr,"About to init SD card, attempt #%d\n",(i+1));
    error = SD_init();
    if(!error) 
    {
      dbgParams.sdcardAvailable = TRUE;
      SD_displayCardInfo(stderr); 
      writeFileFromBuffer("file1.txt","abcdefghijklmnop",strlen("abcdefghijklmnop"));
      break;
    }
  }

  if(error)
  {
    if(error == 1)  
      fprintf(stderr,"SD card not detected..\n");
    if(error == 2)  
      fprintf(stderr,"Card Initialization failed..\n");
  }
#endif

}


//This function is called periodically to sample the physical state of the system
//and updates the corresponding phyParams structure
void updateMeasurements(void)
{
    float voltage=0.0f,current=0.0f;
    if (measureVoltage(&voltage))
    {
      updateVoltageStatistics(voltage);
    }
     else 
     {
      //fprintf(stderr,"Invalid voltage  value detected on the ADC - debug this !!\n");
     }

    if( measureCurrent(&current))
    {
      updateCurrentStatistics(current);
    
      bool coupledBefore = phyParams.isCoupled;    
      phyParams.isCoupled = (fabs(phyParams.currentAmplitude) > COUPLING_THRESHOLD_CURRENT);
      //fprintf(stderr,"CoupledBefore: %d, coupledNow : %d\n",coupledBefore,phyParams.isCoupled);
      if (coupledBefore && !phyParams.isCoupled)
      {
         fprintf(stderr,"Coupling lost, not counting spins till coupling is reestablished\n");
         procedureParams.lostCoupling=TRUE;
         notifyCoupling(FALSE);
      }

      if (!coupledBefore && phyParams.isCoupled)
      {
         fprintf(stderr,"coupling reestablished, counting spins again\n");
         procedureParams.establishedCoupling=TRUE;
        notifyCoupling(TRUE);
      }
     }
     else 
     {
      //fprintf(stderr,"\nInvalid current value detected on the ADC - debug this !!\n");
     }
    
   
    //compute the angular speed : number of spins during the last sampling interval 
    float periodInSeconds = (float)ADC_SAMPLE_INTERVAL/(float)TIME_COUNTS_IN_ONE_SEC;
    phyParams.angularSpeed = (float)(phyParams.spinCount - phyParams.previousSpinCount)/periodInSeconds;
    //fprintf(stderr,"dspin = %d , Angspeed:",(phyParams.spinCount-phyParams.previousSpinCount));
    //printFloat(phyParams.angularSpeed,4,stderr);
    //NEWLINE_STDERR;
    phyParams.previousSpinCount = phyParams.spinCount;

    phyParams.estimatedSpeedConstant = 60*phyParams.angularSpeed/phyParams.voltage;
}


//monitor uart input and perform action according to the commands
void processCommandInput(void)
{
    unsigned char mychar;
    int status;
		unsigned int cc = uart_getc();//COMMAND_INPUT_FUNCTION(); 
    bool isRepeat=FALSE;
		if ( cc & UART_NO_DATA || cc&0xFF00 )
		{
		}	
		else
		{
      mychar = (unsigned char)cc;
      //for debug purposes
      if (dbgParams.duplicateInputToStdErr)
      {
        fprintf(stderr,"%c",cc); 
        fprintf(stdout,"%c",cc); 
      }


      if (mychar==13)
      { 
        cmdParams.cmdBuffer[cmdParams.cmdPosition]=0;      //terminate the string and process the command

        fprintf(stderr,"Dispatching: %s\n",cmdParams.cmdBuffer);
        status=dispatchMessage(cmdParams.cmdBuffer,&phyParams,&cmdParams,&dbgParams,&calibParams,&procedureParams,&isRepeat);
        
        cmdParams.cmdPosition=0;                    //reset the command buffer
        //fprintf(stderr,"\nready for next command \n");
        if (!status)
          fprintf(stderr,"OK\n");
        else
          fprintf(stderr,"ERROR\n");
 
        if (!isRepeat)  
          strcpy(cmdParams.previousCommand,cmdParams.cmdBuffer);

        memset(cmdParams.cmdBuffer,0,MAX_CMD_LEN);
        //printf("\nready for next command \n");
        
      }
      else if (cmdParams.cmdPosition<MAX_CMD_LEN)
      {
        cmdParams.cmdBuffer[cmdParams.cmdPosition] = mychar;
        cmdParams.cmdPosition++;  
      }
   }
}

void updateProcedureState(void)
{
  if (procedureParams.procedureTerminated)
  {
    fprintf(stderr,"btterminated %lu %lu %lu\n",procedureParams.startTime,procedureParams.stopTime,procedureParams.targetSpinCounts);
    fprintf(stderr,"--------------------->Procedure Terminated");
    procedureParams.procedureTerminated=FALSE;
    adjustSpeed(0);
    turnMotorOff();
  }  
}


void processPanelInput(void)
{
  byte buttons=0x0;
  buttons = buttonsPressed();
  bool isRepeat=FALSE;
  if (buttons & PUSH_DOWN)
  {
    waitUntilReleased(PUSH_DOWN);
    fprintf(stderr,"DOWN pressed\n");
    //incrementSettingNumber();
//    dispatchMessage("atdec",&phyParams,&cmdParams,&dbgParams,&calibParams,&procedureParams,&isRepeat);
    //displaySetting(&procedureParams);
    decreaseSpeed(3,&phyParams);
    blink(2);
  
  }
  if (buttons & PUSH_UP)
  {
    waitUntilReleased(PUSH_UP);
    fprintf(stderr,"UP pressed\n");
  //  dispatchMessage("atinc",&phyParams,&cmdParams,&dbgParams,&calibParams,&procedureParams,&isRepeat);
    increaseSpeed(3,&phyParams);
    blink(2);
    //decrementSettingNumber();
    //displaySetting(&procedureParams);
  }
 
  if (buttons & PUSH_RIGHT)
  {
    fprintf(stderr,"RIGHT pressed\n");
    waitUntilReleased(PUSH_RIGHT);
    increaseSpeed(3,&phyParams);
    blink(1);
 }
 
  if (buttons & PUSH_LEFT)
  {
    fprintf(stderr,"LEFT pressed\n");
    waitUntilReleased(PUSH_LEFT);
    decreaseSpeed(3,&phyParams);
    blink(2);
  }

  if (buttons &PUSH_SELECT)
  {
    fprintf(stderr,"SELECT pressed\n");
    waitUntilReleased(PUSH_SELECT);
 
    motorMode=(motorMode+1)%3;

    switch (motorMode)
    {
      case MOTOR_STOPPED:
      {
        phyParams.motorSpeed=HALTING_SPEED_VALUE;
        adjustSpeed(phyParams.motorSpeed);
        turnMotorOff();
        break;
      }
      case MOTOR_CW:
      {
          phyParams.motorSpeed=HALTING_SPEED_VALUE;
          adjustSpeed(phyParams.motorSpeed);
          cbi(PORTA,PA6);
          blink(1);
          turnMotorOn();
          break;
      }
      case MOTOR_CCW:
      {
          phyParams.motorSpeed=HALTING_SPEED_VALUE;
          adjustSpeed(phyParams.motorSpeed);
          sbi(PORTA,PA6);
          blink(2);
          turnMotorOn();
          break;
      }
    } 
    fprintf(stderr,"Shifted dir to %d\n",motorMode);
  }


}

int main(void)
{
  //there is a short in the signal conditioning board (when i connect only 12v vcc to it, i see that the ground, although should be floating
  // it is also ~ 12v, sometimes even higher than my vcc voltage) 
  // for this reason, the first thing i do - i turn on the power mosfet and give power (provide path to the ground) for the signal conditioning board.
  // Once i do that it settles since the resistance from true ground is way smaller so the smaller resistor wins. Ask Mozzi, how to debug this.
  blink(2);
  initPower(); 
	initGeneralSettings();
  initUart();

  fprintf(stderr,"stderr\n");
  fprintf(stdout,"stdout\n");

  initParallax5WayButton();
  //testParallax5WayButton(stderr);

  initPWM();

  initDigitalPot();
  blink(2);
  //testDigitalPot(0x10); 

  initADC();
  //initSDCard();

#if 0
  while (1)
  {
    processCommandInput();
    //testUartInLoopback();
  }
#endif  

  //initCalibration();

  //testADC(0);

	//fprintf(stderr,"Entering uart echo mode\n");
  //while (1)
  //{
  //  testUartInLoopback();
  //}

  int iterationCounter=0;
   

	blink(2);
  while (1)
  {

    processCommandInput();
    processPanelInput();
    iterationCounter++;
    //the ISR signals us once every X milliseconds to resample the system's state
    if (phyParams.shouldSample)
    {  
       //fprintf(stdout,"Sampling time..................................................................\n");
       updateMeasurements();
       //testADCSingleIteration();
       phyParams.shouldSample=FALSE;
    }

    if ( dbgParams.sendPeriodicUpdates && dbgParams.shouldSendUpdate)
    {
      sendUpdates(stderr);
      sendUpdates(stdout);
    }
    
    updateProcedureState();

  }
}

void resetCounters(void)
{
  phyParams.current=0.0;
  phyParams.maxCurrent=0.0;
  phyParams.minCurrent=100000.0;
  phyParams.currentAmplitude=0.0;
  phyParams.ncurrentSamples=0;

  phyParams.voltage=0.0;
  phyParams.minVoltage=100000.0;
  phyParams.maxVoltage=0;
  phyParams.voltageAmplitude=0.0;
  phyParams.nvoltageSamples=0;

  phyParams.isCoupled=FALSE;
}

//this is time-critical and cannot wait for next update. need to send message now !!
void notifyCoupling(bool established)
{
   if (established)
   {
      fprintf(stdout,"btestablishedcoupling\n");
      fprintf(stderr,"btestablishedcoupling\n");
   }
   else
   {
      fprintf(stdout,"btlostcoupling\n");
      fprintf(stderr,"btlostcoupling\n");
   } 

}

//send periodic updates to the console (whoever is listening on the other side of rs232
void sendUpdates(FILE* stream)
{
      dbgParams.shouldSendUpdate=FALSE;
      //int currentValue = (int)(scalingFactor * sin(2.0*3.1416*0.01*(double)msgcnt++));
      //int voltageValue = (int)(scalingFactor * sin(2.0*3.1416*0.01*(double)msgcnt++));
      fprintf(stream,"btcurrent %d %d\n",(int)(phyParams.current*SCALING_FACTOR),SCALING_FACTOR);
      fprintf(stream,"btcurrentamplitude %d %d\n",(int)(phyParams.currentAmplitude*SCALING_FACTOR),SCALING_FACTOR);
      //fprintf(stream,"btvoltage %d %d\n",(int)(phyParams.voltage*SCALING_FACTOR),SCALING_FACTOR);
      //fprintf(stream,"btvoltageamplitude %d %d\n",(int)(phyParams.voltageAmplitude*SCALING_FACTOR),SCALING_FACTOR);
      fprintf(stream,"btspincount %lu\n",phyParams.spinCount);
      //angular speed sent in rpm
      unsigned long int speedRpm = abs ( (unsigned long int) (60*phyParams.angularSpeed));
      fprintf(stream,"btangularspeed %lu %d\n",speedRpm,1);  
      fprintf(stream,"btmseccounter %lu\n",phyParams.msecCounter);

      if (procedureParams.isInProcedure)
      {
         fprintf(stream,"btremaining %lu %lu\n",procedureParams.spinCountsRemaining,procedureParams.targetSpinCounts);
      }

      //debug printouts      
      //fprintf(stderr,"btspincount %lu\n",phyParams.spinCount);
      

      /*fprintf(stderr,"Motor Voltage : ");
      printFloat(phyParams.voltage,6,stderr);
      fprintf(stderr,"\n");

      fprintf(stderr,"Motor Current : ");
      printFloat(1000*phyParams.current,2,stderr);
      fprintf(stderr," [mA]\n");

      fprintf(stderr,"Angular Speed: ");
      printFloat(60*phyParams.angularSpeed,3,stderr);
      fprintf(stderr," RPM\n");*/

      //fprintf(stderr,"Estimated speed constant: ");
      //printFloat(phyParams.estimatedSpeedConstant,6,stderr);
      //NEWLINE_STDERR;

      //fprintf(stderr,"IsInProcedure : %d\n",procedureParams.isInProcedure);
      //fprintf(stderr,"remaining out of total : %lu/%lu\n",procedureParams.spinCountsRemaining,procedureParams.targetSpinCounts);

}

void updateVoltageStatistics(float voltage)
{
      phyParams.voltage = voltage;
      int thisIndex = phyParams.nvoltageSamples%AVG_HISTORY_LENGTH;
      phyParams.voltageSamples[thisIndex]=voltage;

      phyParams.maxVoltage = MAX(phyParams.voltage,phyParams.maxVoltage);
      phyParams.minVoltage = MIN(phyParams.voltage,phyParams.minVoltage);
      //phyParams.voltageAmplitude = fabs( (phyParams.maxVoltage - phyParams.minVoltage)/2.0 ) ;
      //phyParams.voltageAmplitude = (HISTORY_AVG_WEIGHT*phyParams.voltageAmplitude*(phyParams.nvoltageSamples-1)+(1.0-HISTORY_AVG_WEIGHT)*phyParams.voltage)/phyParams.nvoltageSamples;
      
      phyParams.voltageAmplitude=0.0f;
      //right now deal only with the case of full populated array
      if (phyParams.nvoltageSamples>AVG_HISTORY_LENGTH)
      {
        int weight=AVG_HISTORY_LENGTH;
        //go backwards in a circular fashion and compute the weighted average
        for (int i=thisIndex;i>=0;i--)
        {
           phyParams.voltageAmplitude += phyParams.voltageSamples[i]*weight;
           weight--;
        }
        for (int i=AVG_HISTORY_LENGTH-1;i>thisIndex;i--)
        {
           phyParams.voltageAmplitude += phyParams.voltageSamples[i]*weight;
           weight--;
        }
        int totalWeight = (AVG_HISTORY_LENGTH*(AVG_HISTORY_LENGTH+1))/2;
        phyParams.voltageAmplitude /= totalWeight;
      }
      phyParams.nvoltageSamples++;

}


void updateCurrentStatistics(float current)
{
      phyParams.current = current;
      int thisIndex = phyParams.ncurrentSamples%AVG_HISTORY_LENGTH;
      phyParams.currentSamples[thisIndex]=current;
      int icurrent=(int)(1000*phyParams.current);
      //fprintf(stderr,"current : %d ma will go into index#%d\n",icurrent,thisIndex);
      phyParams.maxCurrent = MAX(phyParams.current,phyParams.maxCurrent);
      phyParams.minCurrent = MIN(phyParams.current,phyParams.minCurrent);
      //phyParams.currentAmplitude = fabs ( (phyParams.maxCurrent - phyParams.minCurrent)/2.0 );
      //phyParams.currentAmplitude = (HISTORY_AVG_WEIGHT*phyParams.currentAmplitude*(phyParams.ncurrentSamples-1)+(1.0-HISTORY_AVG_WEIGHT)*phyParams.current)/phyParams.ncurrentSamples;

      phyParams.currentAmplitude=0.0f;
      //right now deal only with the case of full populated array
      if (phyParams.ncurrentSamples>AVG_HISTORY_LENGTH)
      {
        int weight=AVG_HISTORY_LENGTH;
        //go backwards in a circular fashion and compute the weighted average
        for (int i=thisIndex;i>=0;i--)
        {
           phyParams.currentAmplitude += phyParams.currentSamples[i]*weight;
           //fprintf(stderr,"Adding %d ma with weight %d from place %d, totalCurrent=%d ma\n",(int)(1000*phyParams.currentSamples[i]),weight,i,(int)(1000*phyParams.currentAmplitude));
              
           weight--;
        }
        for (int i=AVG_HISTORY_LENGTH-1;i>thisIndex;i--)
        {
           phyParams.currentAmplitude += phyParams.currentSamples[i]*weight;
           //fprintf(stderr,"Adding %d ma with weight %d from place %d, totalCurrent=%d ma\n",(int)(1000*phyParams.currentSamples[i]),weight,i,(int)(1000*phyParams.currentAmplitude));
           weight--;
        }
        int totalWeight = (AVG_HISTORY_LENGTH*(AVG_HISTORY_LENGTH+1))/2;
        //fprintf(stderr,"total current is %d ma, totalWeight : %d\n",(int)(1000*phyParams.currentAmplitude),totalWeight);
        phyParams.currentAmplitude /= totalWeight;
       
      }
      phyParams.ncurrentSamples++;

}




//This is a sample ISR that occurs on a timer overflow event (not in CTC mode)
ISR(TIMER2_OVF_vect)
{
	//phyParams.timecount++;
}

ISR(INT2_vect)
{
	phyParams.spinCount++;				//Increment turns count

  if (procedureParams.isInProcedure && phyParams.isCoupled)
  {
    procedureParams.spinCountsRemaining--;
    if (procedureParams.spinCountsRemaining==0)
    {
      procedureParams.isInProcedure = FALSE;
      procedureParams.stopTime = phyParams.msecCounter;
      procedureParams.procedureTerminated=TRUE;
    }
  }
}

//byte value = 1;
ISR(TIMER2_COMPA_vect)
{
   //To test how often this ISR is called, i toggle PORTB pins
   //PORTB = value;
   //value = 1-value;
   phyParams.msecCounter++;
   if (phyParams.msecCounter%ADC_SAMPLE_INTERVAL==0)
      phyParams.shouldSample = TRUE;
   
   if (phyParams.msecCounter%DATA_UPDATE_INTERVAL==0)
      dbgParams.shouldSendUpdate = TRUE;
    

}
