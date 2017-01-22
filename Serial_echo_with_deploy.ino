 /*
  * Deployment order:
  * >Mechanical remove before flight, Deployment Switch 1 (DS1), DS2 are decompressed until launch, physically inhibit power.
  * >Software Inhibits take over after launch:
  * >Photocell detects light and has delay of 30 minutes after to release antenna (DELAY1)
  * >Antenna deployed
  * >30 minutes delay (DELAY2)
  * >Software switch checks for antenna deployment and RF transmit can begin
  */
  #include <SoftwareSerial.h>
 SoftwareSerial hc12(5,6);
 #define DELAY1 2000 // Delay after photocell detects light
 #define DELAY2 2000 // Delay before antenna is checked for deployment and RF can be turned on 
 #define BURN_TIME 5000 //Time where the heating element will be heated
 #define TEST 1 //Test which enables some LEDs
 bool deploymentActive = true;
 int heatingElement = 3; //heating element's power mosfet is turned on, high current flows through wire
 int photoResistor = 16; //Power is supplied to the variable photo resistor circuit
 int lightLevel = 14; //Light level is low when light is detect, light level is high when it is somewhat dark
 int antennaSwitchPower = 2; //Provides power to the inhibit switch for the antenna
 int antennaSwitchActive = 3; //If high then the antenna has been deployed

 //Test only
 int RFReady = 9; //Signals a purple LED that the inhibit tests is done and good
 int AntennaReleased = 8; //Signals a LED that the antenna is released/wire is burned
 void setup()  
 { 

  pinMode(heatingElement,OUTPUT);
  pinMode(photoResistor,OUTPUT);
  pinMode(lightLevel,INPUT);
  pinMode(antennaSwitchPower,OUTPUT);
  pinMode(antennaSwitchActive,INPUT);
  if (TEST == 1){
    pinMode(RFReady,OUTPUT);
    pinMode(AntennaReleased,OUTPUT);
  }
   
  Serial.begin(9600); 
  hc12.begin(9600);
  //Serial.print(0);
  
 }  
 void loop()  
 {  
//  One time code post deployment
  if(deploymentActive == true){
    deploymentActive = false;
    digitalWrite(photoResistor, HIGH);
    while(digitalRead(lightLevel) == HIGH){ //While no light is detected, delay
      delay(10);
    }
    //light has been detected
    digitalWrite(photoResistor,LOW); //turn off pin
    delay(DELAY1);
    if (TEST == 1){
      digitalWrite(AntennaReleased, HIGH);
      delay(BURN_TIME);
      digitalWrite(AntennaReleased, LOW);
    }
    delay(DELAY2);
    digitalWrite(antennaSwitchPower,HIGH);
    while(digitalRead(antennaSwitchActive) == LOW){ //While switch is not depressed, delay
      delay(10);
    }
    digitalWrite(antennaSwitchPower,LOW); //turn off pin
    if (TEST == 1){
      digitalWrite(RFReady, HIGH);
    }
  
    
  }
//  Code during flight goes here
/*
  if(Serial.available())  
  {  
   char c = Serial.read();  
   Serial.print(c);  
  }  
  */
     if(Serial.available()) hc12.write(Serial.read());
  if(hc12.available()) Serial.write(hc12.read());
 }  
