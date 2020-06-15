/*
 * NitSpec scope hardware (Nano) ATmega
 * (C) Nitramite - Martin K.
 */
// --------------------------------------------------------------------------

// ## Libraries ##
#include <Servo.h>
#include <SFE_BMP180.h>
#include <Wire.h>

// --------------------------------------------------------------------------

// ## Pins ##
int selectTargetBtnPin  = 2;
int ledGreenPin         = 3;
int triggerServoPin     = 9;
int voltageSensorPin    = A6;

// --------------------------------------------------------------------------
// ## Sensor readings ##

double pressureValue = 0.0;
double temperatureValue = 0.0;
double voltageValue = 0.0;

// --------------------------------------------------------------------------

// ## Variables ##
Servo triggerServoObj;  // Create servo object to control a servo
int trigServPos = 0;    // Variable to store the servo position
int selectTargetBtnPressAcknowledged = 1; // Select target button press acknowledged by controller
SFE_BMP180 bmpPressure;
// #define ALTITUDE 65.0 // Altitude of your device location from sea level in meters (SASTAMALA)
#define ALTITUDE 96.0 // Altitude of your device location from sea level in meters (TAMPERE)
double T,P,p0,a;
const unsigned long sensorsReadInterval = 30 * 1000UL; // 30 seconds (to make minutes use like: 2 * 60 * 1000UL;)
unsigned long sensorsLastReadMillis = 0;

// --------------------------------------------------------------------------
/* Setup and loop */


// SETUP
void setup() {
  Serial.begin(9600); // Sets the baud for serial data transmission
  pinMode(selectTargetBtnPin, INPUT_PULLUP); // Set select target button to input pullup mode
  triggerServoObj.attach(triggerServoPin);
  triggerServoObj.write(180); // To zero position
  triggerServoObj.detach();

  // init BMP180 i2c chip
  if (bmpPressure.begin() == true)
    Serial.println("BMP180 init success");
  else {
    Serial.println("BMP180 init fail\n\n");
  }

  // Set led pin as output
  pinMode(ledGreenPin, OUTPUT);           digitalWrite(ledGreenPin, LOW); 
}


// LOOP
void loop() {
  // put your main code here, to run repeatedly:
  delay(100);

  // Look for select target button press
  int selectTargetBtnState = digitalRead(selectTargetBtnPin); // Get current state
  if (selectTargetBtnState == LOW && selectTargetBtnPressAcknowledged == 1) { // Pressed down
    selectTargetBtnPressAcknowledged = 0; // Set false to wait for controller response
    Serial.print((String)"TRIGGER" + "\n\r"); // Send select target command to controller
  }

  // Listen for available commands
  if(Serial.available() > 0) {
    char inputData = Serial.read();
    if(inputData == '6') {
      triggerAction();
    }
    else
    if (inputData == '7') {
      selectTargetBtnPressAcknowledged = 1; // Controller awknoledged target selection
    }
  }

  // Read and send sensor, laser range finder etc readings
  /**
   * 
   */

   unsigned long now = millis();
   if (now - sensorsLastReadMillis >= sensorsReadInterval) {
      // digitalWrite(ledGreenPin, HIGH); // On
      sensorsLastReadMillis += sensorsReadInterval;
      readPressureSensors(); // Do BMP reading
      voltageRead(); // Read power source voltage

      // Send sensor readings to controller
      Serial.print((String)
        "PRS:" + pressureValue + ";" + 
        "TMP:" + temperatureValue + ";" +
        "VTG:" + voltageValue + ";" +
        "\n\r"
      );
      // digitalWrite(ledGreenPin, LOW); // Off
   }

}

// --------------------------------------------------------------------------------------------------------------------------
// -------------------------------------A L L - A C T I O N - F U C T I O N S------------------------------------------------
// --------------------------------------------------------------------------------------------------------------------------

/* Action functions */
// 14.02.2019 - changed moving to move only 50 degrees
// 
// Move servo to fire position and back to 'home' position
void triggerAction() {
  // digitalWrite(ledGreenPin, HIGH); // On
  triggerServoObj.attach(triggerServoPin);
  for (trigServPos = 180; trigServPos >= 130; trigServPos -= 1) { // goes from 180 degrees to 110 degrees
    // in steps of 1 degree
    triggerServoObj.write(trigServPos);              // tell servo to go to position in variable 'pos'
    delay(4);                       // waits 15ms for the servo to reach the position
  }
  delay(500);
  for (trigServPos = 130; trigServPos <= 180; trigServPos += 1) { // goes from 110 degrees to 180 degrees
    triggerServoObj.write(trigServPos);              // tell servo to go to position in variable 'pos'
    delay(4);                       // waits 15ms for the servo to reach the position
  }
  triggerServoObj.detach();
  // digitalWrite(ledGreenPin, LOW); // Off
}


// --------------------------------------------------------------------------------------------------------------------------
// -------------------------------------A L L - S E N S O R - R E A D I N G - T A S K S -------------------------------------
// --------------------------------------------------------------------------------------------------------------------------


// ### READ PRESSURE SENSOR ###
void readPressureSensors() {
  char status;
  status = bmpPressure.startTemperature();
  if (status != 0) {
    // Wait for the measurement to complete:
    delay(status);
    status = bmpPressure.getTemperature(T);
    if (status != 0) {
      //Serial.print("BMP180 temperature: ");
      //Serial.print(T,2);
      temperatureValue = T,2;
      //Serial.print(" deg C, ");
      //Serial.print((9.0/5.0)*T+32.0,2);
      //Serial.println(" deg F");
      status = bmpPressure.startPressure(3); // The parameter is the oversampling setting, from 0 to 3 (highest res, longest wait).
      if (status != 0) {
        delay(status); // Wait for the measurement to complete:
        status = bmpPressure.getPressure(P,T); // P = kPa/kiloPascals
        if (status != 0) {
          //Serial.print("Absolute pressure: ");
          //Serial.print(P,2);
          //Serial.print(" mb, ");
          //Serial.print(P*0.0295333727,2);
          //Serial.println(" inHg");
          pressureValue = P; // Store to variable          
          //Serial.print("pressure_sensor_01_value = ");
          //Serial.println(P);
          p0 = bmpPressure.sealevel(P,ALTITUDE);
          //Serial.print("Relative (sea-level) pressure: ");
          //Serial.print(p0,2);
          //Serial.print(" mb, ");
          //Serial.print(p0*0.0295333727,2);
          //Serial.println(" inHg");
          a = bmpPressure.altitude(P,p0);
          //Serial.print("computed altitude: ");
          //Serial.print(a,0);
          //Serial.print(" meters, ");
          //Serial.print(a*3.28084,0);
          //Serial.println(" feet");
        }
        else Serial.println("error retrieving pressure measurement\n");
      }
      else Serial.println("error starting pressure measurement\n");
      }
      else Serial.println("error retrieving temperature measurement\n");
    }
    else Serial.println("error starting temperature measurement\n");
}

// --------------------------------------------------------------------------------
// ### READ SYSTEM VOLTAGES | 12v | 5v | ###

void voltageRead() {
  // 12v
  float vTemp;
  int vRead = analogRead(voltageSensorPin);
  vTemp = vRead/4.092; // Latter value was related to resistor resistance
  voltageValue = (vTemp/10); // Into voltage
  //Serial.print("Voltage val : ");
  //Serial.println(voltageValue);
  //Serial.println("V");
  
  // 5v
  /*
  float vLowTemp;
  int vLowRead = analogRead(voltageLowSensorPin);
  vLowTemp = vLowRead/4.092; // Latter value was related to resistor resistance
  voltage_low_sensor_value = (vLowTemp/10); // Into voltage
  Serial.print("Voltage 5v val : ");
  Serial.println(voltage_low_sensor_value);
  Serial.println("V");
  Serial.println("");
  */
}

// --------------------------------------------------------------------------------
