 #include <SoftwareSerial.h>
 SoftwareSerial hc12(5,6);
 void setup()  
 {  
  Serial.begin(9600); 
  hc12.begin(9600);
  //Serial.print(0); 
 }  
 void loop()  
 {  
 // if(Serial.available())  
//  {  
 //  char c = Serial.read();  
   //Serial.print(c);  
  //}  
   if(Serial.available()) hc12.write(Serial.read());
  if(hc12.available()) Serial.write(hc12.read());
 }  
