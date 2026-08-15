#ifndef UTILITY_H
#define UTILITY_H

// Definitions
#define WAIT_SHORT 1000

//delays the controller for ncycles cycles by incrementing a counter
void delayCycles(unsigned long ncycles);

//blinks n times with delay of dd milliseconds between the blinks.
void blink(int n);

//blink on a given bit of portA, n times
void blinkOnPortA(int bit,int n);

void infiniteLoop(void);

void infiniteLoopBlinking(void);

#endif
