#include "printFloat.h"

void printFloat(float value, int places, FILE* dstStream)
{
 // this is used to cast digits
 int digit;
 float tens = 0.1;
 int tenscount = 0;
 int i;
 float tempfloat = value;

 // if value is negative, set tempfloat to the abs value

   // make sure we round properly. this could use pow from
 //<math.h>, but doesn't seem worth the import
 // if this rounding step isn't here, the value  54.321 prints as

 // calculate rounding term d:   0.5/pow(10,places)
 float d = 0.5;
 if (value < 0)
   d *= -1.0;
 // divide by ten for each decimal place
 for (i = 0; i < places; i++)
   d/= 10.0;
 // this small addition, combined with truncation will round our

 tempfloat +=  d;

 if (value < 0)
   tempfloat *= -1.0;
 while ((tens * 10.0) <= tempfloat) {
   tens *= 10.0;
   tenscount += 1;
 }

 // write out the negative if needed
 if (value < 0)
   fprintf(dstStream,"-");

 if (tenscount == 0)
   fprintf(dstStream,"0");

 for (i=0; i< tenscount; i++) {
   digit = (int) (tempfloat/tens);
   fprintf(dstStream,"%d",digit);
   tempfloat = tempfloat - ((float)digit * tens);
   tens /= 10.0;
 }

 // if no places after decimal, stop now and return
 if (places <= 0)
   return;

 // otherwise, write the point and continue on
 fprintf(dstStream,".");

 for (i = 0; i < places; i++) {
   tempfloat *= 10.0;
   digit = (int) tempfloat;
   fprintf(dstStream,"%d",digit);
   // once written, subtract off that digit
   tempfloat = tempfloat - (float) digit;
 }
}


