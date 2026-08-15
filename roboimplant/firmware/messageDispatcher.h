#ifndef MESSAGE_DISPATCHER_H
#define MESSAGE_DISPATCHER_H

#include "config.h"

typedef int (*messageListenerHandler)(float, char, char);


//Initialize the message dispatcher
void initMessageDispatcher(void);

//once a message has arrived - dispatch it.
int dispatchMessage(char* message,PhysicalParamsBlock* pparams,CommandParams* cmdParams,DebugParams* dbgParams, CalibrationParams* pcalibParams, ProcedureParams* pprocParams,bool* pisRepeat);


//Who will get a notification once a message arrives
void registerMessageListener(messageListenerHandler handler);

void increaseSpeed(int value,PhysicalParamsBlock* pparams);
void decreaseSpeed(int value,PhysicalParamsBlock* pparams);

#endif
