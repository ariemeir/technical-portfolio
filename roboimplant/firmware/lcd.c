void initiLCD()
//initialize the LCD
{
    P3 = 1;
    P1 = 1;
    RST = 0;
    delay(2);
    RST = 1;
    delay(20);
    Writecom(0x30);
    delay(2);
    Call writecom(0x30);
    Call writecom(0x30);
    Call writecom(0x39);
    Call writecom(0x14);
    Call writecom(0x56);
    Call writecom(0x6D);
    Call writecom(0x70);
    Call writecom(0x0C);
    Call writecom(0x06);
    Call writecom(0x01);
    delay(10);
}
//RESET
//end reset
//wake up
//wake up
//wake up
//function set
//internal osc frequency
//power control
//follower control
//contrast
//display on
//entry mode
//clear
void writecom(int d)
{
    CS = 0;
    //CS
    RS = 0;
    //A0 = Command
    for(serialcounter = 1; serialcounter <= 8; serialcounter++) //send 8 bits
    {
      if((d&0x80)==0x80)
        //get only the MSB
        SI=1;
      //if 1, then SI=1
      else
        SI=0;
      //if 0, then SI=0
      d=(d<<1);
      //shift data byte left
      SCL = 0;
      SCL = 1;
      SCL = 0;
      //SCL
    }
    CS = 1;
}
void writedata(int d)
{
  CS = 0;
  //CS
  RS = 1;
  //A0 = Data
  for(serialcounter = 1; serialcounter <= 8; serialcounter++) //send 8 bits
  {
    if((d&0x80)==0x80)
      //get only the MSB
      SI=1;
      //if 1, then SI=1
    else
      SI=0;
      //if 0, then SI=0
    d=(d<<1);
    //shift data byte left
    SCL = 0;
    SCL = 1;
    SCL = 0;
    //SCL
  }
  CS = 1;
}

