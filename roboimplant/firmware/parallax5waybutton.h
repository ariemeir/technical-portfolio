#ifndef PARALLAX_5WAY_BUTTON
#define PARALLAX_5WAY_BUTTON

#include "config.h"

#define PUSH_DOWN    0x01
#define PUSH_UP      0x02
#define PUSH_LEFT    0x04
#define PUSH_RIGHT   0x08
#define PUSH_SELECT  0x10

void initParallax5WayButton(void);
void testParallax5WayButton(FILE* outstream);

bool isLeftPressed(void);
bool isRightPressed(void);
bool isUpPressed(void);
bool isDownPressed(void);
bool isSelectPressed(void);

byte buttonsPressed(void);

void waitUntilReleased(byte button);
void waitUntilPressed(byte button);
void waitUntilAllButtonsReleased(void);
#endif

