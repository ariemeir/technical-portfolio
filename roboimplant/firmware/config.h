#ifndef ROBOIMPLANT_CONFIG_H
#define ROBOIMPLANT_CONFIG_H

#include <avr/io.h>
#include <stdio.h>
#include <util/delay.h>
#include <avr/interrupt.h>


//Maximum length for a command sent over UART
#define MAX_CMD_LEN 256
#define TRUE        1
#define FALSE       0
// to avoid dealing with floats, i scale everything by this factor when data exchanging with the console
#define SCALING_FACTOR 1000

//some useful macros
#define MIN(a,b) (((a)<(b))?(a):(b))
#define MAX(a,b) (((a)>(b))?(a):(b))

//Set bit in IO port 
#define sbi(port, bit) (port) |= (1 << (bit))

//Clear \c bit in IO port \c port.
#define cbi(port, bit) (port) &= ~(1 << (bit))



// units mNm/A
#define TORQUE_CONSTANT   (23.5)  

// units rpm/V
#define SPEED_CONSTANT    (406.0) 

//threshold above which, the motor is considered coupled to the implant
#define COUPLING_THRESHOLD_CURRENT (0.035)

#define HIGH_SPEED          255
#define LOW_SPEED           0

//How many rotations do we need to make to shift the rod by 1mm.
//#define MM_TO_ROTATIONS_FACTOR    (1024.0*20.0)
#define MM_TO_ROTATIONS_FACTOR    (1.0)


// this is the resistance value that leads to speed effectively 0
#define HALTING_SPEED_VALUE   110

#define CLOCK_WISE          0
#define COUNTER_CLOCK_WISE  1

// how many time counts (timer#2 interrupts) occurs in a single second (this might be 1024 to be more precise)
#define TIME_COUNTS_IN_ONE_SEC   (1000)

//how often (in msec) should we update the console about the state of the system
#define DATA_UPDATE_INTERVAL    2500

//for cumulative moving average (alpha*old_average + (1-alpha)*new_sample)
#define HISTORY_AVG_WEIGHT    0.5

//Auxiliary type declarations
typedef uint8_t byte;
typedef uint8_t bool;
typedef volatile float vfloat;
typedef volatile int vint;

#define AVG_HISTORY_LENGTH    10

//This struct describes the physical parameters of the system,
//such as the different mechanical/electrical state variables
//that are periodically sampled (or computed)
typedef struct _params_block
{
  volatile int motorSpeed;
  volatile float voltage;
  volatile float maxVoltage;
  volatile float minVoltage;
  volatile float voltageAmplitude;

  volatile float current;
  volatile float maxCurrent;
  volatile float minCurrent;
  volatile float currentAmplitude;

  //how many turns of the motor have we witnessed
  volatile long int spinCount;
  volatile long int previousSpinCount;

  //counts the time ticks using the timer interrupt mechanism.
  //due to 16mhz clock, each interrupt actulaly occurs every 1.024 msec - this is why J.L. used 32khz external crystal
  volatile unsigned long int msecCounter;

  //this is a flag turned by the timer ISR indicating that the period between adc samples has elapsed.
  volatile bool shouldSample;

  //Those are characteristic values of the motor that allow translating speed and torque from voltage and current
  volatile double torqueConstant;
  volatile double speedConstant;

  //Is the implant coupled to the motor at a given time (measured via current thresholding)
  volatile bool isCoupled;
  
  //are we going cw or ccw
  volatile byte rotationDirection;


  //angular speed in RPS (rounds per second)
  volatile float angularSpeed;

  //dividing the speed by the voltage, we can try and see how our estimate does with the spec
  volatile float estimatedSpeedConstant;

  volatile int ncurrentSamples;
  volatile float currentSamples[AVG_HISTORY_LENGTH];

  volatile int nvoltageSamples;
  volatile float voltageSamples[AVG_HISTORY_LENGTH];

} PhysicalParamsBlock;


//This struct describes the  parameters of the interaction between the user
//and the console (typically over a uart serial port BT/RS232)
typedef struct _command_param_block
{
  //This buffer contains the command that user sends to us over uart
  char previousCommand[MAX_CMD_LEN];
  char cmdBuffer[MAX_CMD_LEN];
  //current position inside the command buffer
  int cmdPosition;
}
CommandParams;

//this struct aggregates all the different calibration parameters
typedef struct _calibration_param_block
{
  //motor voltage on low speed setting
  float lowSpeedVoltage;
  //motor voltage on high speed setting
  float highSpeedVoltage;
  
  //motor current without any load
  float noloadCurrent;
  //moto current when loaded with implant
  float loadedCurrent;

  //motor current during low and high speeds
  float lowSpeedCurrent;
  float highSpeedCurrent;

  //what percentage of the loadedCurrent should be considered coupled
  float currentThresholdPercentage;  
}
CalibrationParams;


//This struct aggregates all the different debug related parameters
typedef struct _debug_param_block
{
  bool duplicateInputToStdErr;
  bool sendPeriodicUpdates;
  bool sdcardAvailable;
  bool shouldSendUpdate;
  //how often should we send an update
  int updateFrequencyMsec;
  char currentTime[30];
}
DebugParams;


typedef struct procedure_param_block
{
  //specifies that we are in the middle of a procedure, all statistics are reset prior to this
  volatile bool isInProcedure;

  //specifies that the procedure has been terminated and some info has to be sent to the console
  volatile bool procedureTerminated;

  //once we are in procedural mode, our mission is to extend the implant by dosage (can be negative for shortening of the rod)
  volatile float dosage;

  //remember - if we have no RTC, our clock is the 1msec ticks from the timer interrupt 
  // these variables keep our start/stop time
  volatile unsigned long int startTime;
  volatile unsigned long int stopTime;

  //how many spin counts do we have to do for this procedure
  volatile unsigned long int targetSpinCounts;  
  
  //how many rotations of the motor do we have left.
  volatile unsigned long int spinCountsRemaining;

  volatile bool lostCoupling;
  volatile bool establishedCoupling;
}
ProcedureParams;

extern void setDefaultParamValues(void);
void resetCounters(void);
void sendUpdates(FILE* stream);
//coupling is an urgent matter and any change in its status has to be delivered to the console a.s.a.p
void notifyCoupling(bool established);

void updateVoltageStatistics(float voltage);
void updateCurrentStatistics(float current);

#endif
