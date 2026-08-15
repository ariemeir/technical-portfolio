#include "messageDispatcher.h"
#include "utils.h"
#include "printFloat.h"
#include "pwm.h"
#include "power.h"
#include <string.h>
#include <ctype.h>
#include <stdlib.h>
#include <stdio.h>
#include "digitalPot.h"

//this string trims the white space in a string
char *trimWhiteSpace(char *str)
{
  char *end;

  // Trim leading space
  while(isspace(*str)) str++;

  if(*str == 0)  // All spaces?
    return str;

  // Trim trailing space
  end = str + strlen(str) - 1;
  while(end > str && isspace(*end)) end--;

  // Write new null terminator
  *(end+1) = 0;

  return str;
}


void increaseSpeed(int value,PhysicalParamsBlock* pparams)
{
     fprintf(stdout,"Increasing speed by %d\n",value);
     pparams->motorSpeed = MIN(pparams->motorSpeed+value,0xFF);
     //setPWMOutput(pparams->motorSpeed);
     adjustSpeed(pparams->motorSpeed);
}

void decreaseSpeed(int value,PhysicalParamsBlock* pparams)
{
     fprintf(stderr,"Decreasing speed by %d\n",value);
     if (pparams->motorSpeed>value)
        pparams->motorSpeed-= value;
     else
        pparams->motorSpeed = 0;

     //setPWMOutput(pparams->motorSpeed);
     adjustSpeed(pparams->motorSpeed);
}





int dispatchMessage(char* message, PhysicalParamsBlock* pparams, CommandParams* cmdParams,DebugParams* pdbgParams, CalibrationParams* pCalibParams, ProcedureParams* pprocParams, bool* pisRepeat)
{
  int value=0;
  float fvalue=0.0;
  char* args=NULL;
  char* trimmedArgs=NULL;
  *pisRepeat=FALSE;

  int len = strlen(message);
  for (int i = 0; i<len; i++)
    message[i] = tolower(message[i]);

  if (len<3)
  {
    //fprintf(stderr,"ERROR\n");
    //fprintf(stderr,"\nInvalid message: %s\n",message);
    //fprintf(stdout,"\nInvalid message: %s\n",message);
    return -1;
  }
  fprintf(stderr,"Dispatching message:%s\n",message);
  
  if (message[0]!='a' || message[1]!='t')
  {
    //fprintf(stderr,"ERROR\n");
    //fprintf(stderr,"\nInvalid message: %s\n",message);
    //fprintf(stdout,"\nInvalid message: %s\n",message);
    return -1;
  }
  //for (int i=0;i<len;i++)
  //  fprintf(stderr,"message[%d]=%c\n",i,message[i]);
  message = message+2;  //ignore the at prefix, start from here
  //fprintf(stderr,"message is : %s\n",message);

  // ATI - prints info 
  if (strncmp(message,"i",strlen("i"))==0&& message[2]==0)  //check null termination due to prefix possibility
  {
     extern void updateMeasurements();
     updateMeasurements();
     fprintf(stderr,"motorSpeed=%d, inst.voltage=",pparams->motorSpeed);
     printFloat(pparams->voltage,6,stderr);
     fprintf(stderr,",voltageAmplitude=");
     printFloat(pparams->voltageAmplitude,6,stderr);
     fprintf(stderr,",inst.current=");
     printFloat(pparams->current,6,stderr);
     fprintf(stderr,",currentAmplitude=");
     printFloat(pparams->currentAmplitude,6,stderr);
     NEWLINE_STDERR;
     return 0; 
  }

  // ATINC - increments motor speed
  if (strncmp(message,"inc",strlen("inc"))==0)
  {
     args=message+strlen("inc");
     trimmedArgs = trimWhiteSpace(args);
     if (strlen(trimmedArgs)>0)
        value = atoi(trimmedArgs);
     else
        value = 1;
     
     increaseSpeed(value,pparams);
     return 0;  
  }
  // ATDEC - decrements motor speed
  if (strncmp(message,"dec",strlen("dec"))==0)
  {
     args=message+strlen("dec");
     trimmedArgs = trimWhiteSpace(args);
     if (strlen(trimmedArgs)>0)
        value = atoi(trimmedArgs);
     else
        value = 1;
     decreaseSpeed(value,pparams);
     return 0;
  }  
 
  // ATBLINK - blinks led
  if (strncmp(message,"blink",strlen("blink"))==0)
  {
    //fprintf(stderr,"\nGot blink\n");
    blink(1);
    return 0;
  }

  // ATSPEED X - sets a new motor speed
  if (strncmp(message,"speed",strlen("speed"))==0)
  {
     args=message+strlen("speed");
     trimmedArgs = trimWhiteSpace(args);
     if (strlen(trimmedArgs)>0) 
     {
      int newSpeed = atoi(trimmedArgs);
      fprintf(stderr,"new speed was received:%d\n",newSpeed);
      pparams->motorSpeed = newSpeed;
      //update controller with the new speed
      //setPWMOutput(pparams->motorSpeed);
      adjustSpeed(pparams->motorSpeed);
     } 
     return 0;
  }
  
  if (strncmp(message,"updateinterval",strlen("updateinterval"))==0)
  {
     args=message+strlen("updateinterval");
     trimmedArgs = trimWhiteSpace(args);
     if (strlen(trimmedArgs)>0) 
        value = atoi(trimmedArgs);
     else
        value = DATA_UPDATE_INTERVAL;  //if no parameter was passed, the 
     pdbgParams->updateFrequencyMsec = value;
     fprintf(stderr,"After atupdateinterval: updateFrequencyMsec=%d\n",pdbgParams->updateFrequencyMsec);
     return 0;
  }

  // ATUPDATE - enables (atupdate 1) or disables (atupdate 0) periodic updates that the controller sends to uart
  if (strncmp(message,"update",strlen("update"))==0)
  {
     args=message+strlen("update");
     trimmedArgs = trimWhiteSpace(args);
     if (strlen(trimmedArgs)>0) 
        value = atoi(trimmedArgs);
     else
        value = 0;  //if no parameter was passed, the updates are shut-off
  
     pdbgParams->sendPeriodicUpdates=(value==0)?(FALSE):(TRUE);
     fprintf(stderr,"sendPeriodicUpdates=%d\n",pdbgParams->sendPeriodicUpdates);
     fprintf(stderr,"updateFrequencyMsec=%d\n",pdbgParams->updateFrequencyMsec);
     return 0;
  }
  if (strncmp(message,"reset",strlen("reset"))==0)
  {
     resetCounters();  
     return 0;
  }

  // ATR - repeat previous command
  if (strncmp(message,"r",strlen("r"))==0 && message[1]==0)
  {
    //fprintf(stderr,"Repeating previous command\n");
    dispatchMessage(cmdParams->previousCommand,pparams,cmdParams,pdbgParams,pCalibParams,pprocParams,pisRepeat);  
    *pisRepeat=TRUE;
    return 0;
  }

  if (strncmp(message,"u",strlen("u"))==0 && message[1]==0)
  {
    pdbgParams->sendPeriodicUpdates = 1-pdbgParams->sendPeriodicUpdates;  
    return 0;
  }
 
  if (strncmp(message,"motoron",strlen("motoron"))==0)
  {
     fprintf(stderr,"Motor turning ON\n");
     fprintf(stdout,"Motor turning ON\n");
     //resetSpeed();
     turnMotorOn();
     _delay_ms(10);
     return 0;
  }
  if (strncmp(message,"motoroff",strlen("motoroff"))==0)
  {
     fprintf(stderr,"Motor turning OFF\n");
     fprintf(stdout,"Motor turning OFF\n");
     setPWMOutput(0);
     _delay_ms(100);
     turnMotorOff();
     return 0;
  }
  
  if (strncmp(message,"dosage",strlen("dosage"))==0)
  {
     args=message+strlen("dosage");
     trimmedArgs = trimWhiteSpace(args);
     if (strlen(trimmedArgs)>0) 
        fvalue = atof(trimmedArgs);
     else
        fvalue = 0.0;  //if no parameter was passed, the 
     
     pprocParams->dosage = fvalue;
     fprintf(stderr,"Dosage received: ");
     printFloat(fvalue,3,stderr);
     NEWLINE_STDERR;
    
     pprocParams->targetSpinCounts = (int) (pprocParams->dosage*MM_TO_ROTATIONS_FACTOR);
     fprintf(stderr,"Target spin counts : %ld\n",pprocParams->targetSpinCounts);
     pprocParams->spinCountsRemaining = pprocParams->targetSpinCounts;
     return 0;
  }
  if (strncmp(message,"start",strlen("start"))==0)
  {
    fprintf(stderr,"===============> Acquisition begins\n");
    pprocParams->isInProcedure=TRUE;
    resetCounters();
    //notifyCoupling(pparams->isCoupled);
    return 0;
  }
  if (strncmp(message,"stop",strlen("stop"))==0)
  {
    pprocParams->isInProcedure=FALSE;
    return 0;
  }

  if (strncmp(message,"settime",strlen("settime"))==0)
  {
    args=message+strlen("settime");
    trimmedArgs = trimWhiteSpace(args);
    strcpy(pdbgParams->currentTime,trimmedArgs);
    fprintf(stderr,"Current time was set to : %s\n",pdbgParams->currentTime);
    return 0;   
  } 
  fprintf(stderr,"Testing for new commands\n");
  if (strncmp(message,"pwm",strlen("pwm"))==0)
  {
    args=message+strlen("pwm");
    trimmedArgs = trimWhiteSpace(args);
    value=atoi(trimmedArgs);
    fprintf(stderr,"Setting pwm duty cycle to %d out of 255 \n",value);
    setPWMOutput(value);
    return 0;
  } 
  if (strncmp(message,"enable",strlen("enable"))==0)
  {

    fprintf(stderr,"Writing the enable bit to 1\n");
    turnMotorOn(); 
    return 0;
  }
  if (strncmp(message,"disable",strlen("disable"))==0)
  {
    fprintf(stderr,"Writing the enable bit to 0\n");
    cbi(PORTA,PA5);
    return 0;
  }


  if (strncmp(message,"cw",strlen("cw"))==0)
  {
    fprintf(stderr,"Writing the cw-ccw bit to 1\n");
    sbi(PORTA,PA6);
    return 0;
  }
  
  if (strncmp(message,"ccw",strlen("ccw"))==0)
  {
    fprintf(stderr,"Writing the cw-ccw bit to 0\n");
    cbi(PORTA,PA6);
    return 0;
  }
  

  fprintf(stderr,"Message unrecognized: %s\n",message);
  return -1;
}

