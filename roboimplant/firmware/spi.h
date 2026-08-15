#ifndef ROBOIMPLANT_SPI_H
#define ROBOIMPLANT_SPI_H


void SPI_Init(void);
void SPI_Transmit(unsigned char data);

unsigned char SPI_Receive(void);

void SPI_Test(void);

#endif
