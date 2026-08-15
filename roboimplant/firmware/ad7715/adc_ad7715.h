#define SCLK_1  (PORTE|=0x04) 
#define SCLK_0  (PORTE&=~0x04) 
#define DAT_1   (PORTE|=0x08) 
#define DAT_0   (PORTE&=~0x08) 
#define DAT_DIR_OUT  (DDRE|=0x08) 
#define DAT_DIR_IN   (DDRE&=~0x08) 
#define DAT_IN       (PINE&=0x08) 
#define AD7715_Int_ON  (EIMSK |= 0x10) 
#define AD7715_Int_STOP  (EIMSK &= ~0x10) 
 
extern void Delayx10ms(unsigned int count); 
extern void wrad7715(unsigned char cod); 
extern unsigned int rddata(void); 
extern void Init_AD7715(void);     
extern void Channel(unsigned char channel); 
 
extern unsigned int ADC_buffer; 
extern unsigned char Ready;
