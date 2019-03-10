/*
  Analog input, analog output, serial output

  Reads an analog input pin, maps the result to a range from 0 to 255 and uses
  the result to set the pulse width modulation (PWM) of an output pin.
  Also prints the results to the Serial Monitor.

  The circuit:
  - potentiometer connected to analog pin 0.
    Center pin of the potentiometer goes to the analog pin.
    side pins of the potentiometer go to +5V and ground
  - LED connected from digital pin 9 to ground

  created 29 Dec. 2008
  modified 9 Apr 2012
  by Tom Igoe

  This example code is in the public domain.

  http://www.arduino.cc/en/Tutorial/AnalogInOutSerial
*/

#define IS_DEBUG (1)

#define FILTER_NUM  (10) // filter_array number
#define MEMORY_NUM (5) // memory_array number
#define PRINT_NUM (500) // interval to print (about 10ms per count)
#define HOLD_NUM (300) // interval to transit next state (about 10ms per count)
#define INIT_NUM (500) // interval to transit init state (about 10ms per count)

// These constants won't change. They're used to give names to the pins used:
const int analog_pin = A0;  // Analog input pin that the potentiometer is attached to


int filter_array[FILTER_NUM] = {0};
int memory_array[MEMORY_NUM] = {0};

int filter_counter = 0;
int memory_counter_r = 1;
int memory_counter_w = 0;
int state_counter = 0;
int print_counter = 0;

int zero_offset_val = 776; // [value] @ 0kg
int val_to_gram = 275; // [g] per [value]

int old_val = 0;
int dif_val = 0;
int state = 0;

int sensor_val = 0; 
int filter_val = 0;
int memory_val = 0;

float memory_kg = 0.0;
float current_kg = 0.0;

void setup() {
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  // initialize serial communications at 9600 bps:
  Serial.begin(9600);
}

void loop() {
  // count up
  filter_counter++;
  memory_counter_r++;
  memory_counter_w++;
  print_counter++;
  if(filter_counter>=FILTER_NUM) filter_counter=0;
  if(memory_counter_r>=MEMORY_NUM) memory_counter_r=0;
  if(memory_counter_w>=MEMORY_NUM) memory_counter_w=0;
  if(print_counter>=PRINT_NUM) print_counter=0;
  
  // read the analog in value:
  sensor_val = analogRead(analog_pin);
  
  //  filtering
  filter_array[filter_counter] = sensor_val;
  filter_val = 0;
  for(int i=0; i<FILTER_NUM; i++) filter_val += filter_array[i];
  filter_val = (int)(filter_val/FILTER_NUM);

//  diff
dif_val = filter_val - old_val;
old_val = filter_val;

// memory
memory_array[memory_counter_w] = filter_val;


switch(state){
  case 0:
    // state change (hold)
    if( 500 < filter_val && filter_val < 700 ){
      state_counter++;
            
      if( state_counter >= HOLD_NUM ){
        state_counter = 0;
        state = 1;
      }

    }
    break;
  
  case 1:
    // state change (measure)     ref) 60kg = about 560
    if( dif_val < -10 ){
      state = 2;
      memory_val = memory_array[memory_counter_r];
      digitalWrite(LED_BUILTIN, HIGH);
    }
    break;

  case 2:
    // state change (init)    ref) ave. = about 305
    if( 300 < filter_val && filter_val < 400 ){
      state_counter++;

      if(state_counter>=INIT_NUM){
        state_counter = 0;
        state = 0;
        digitalWrite(LED_BUILTIN, LOW);
      }
    }
    break;

  default:
  ;
}

  // mapping
  memory_kg = val_to_gram/1000.0*(float)map(memory_val, 0, 1023, zero_offset_val, zero_offset_val-1023);
  current_kg = val_to_gram/1000.0*(float)map(filter_val, 0, 1023, zero_offset_val, zero_offset_val-1023);

if(!IS_DEBUG && print_counter==0){
  // transmit the results to the Mobile Application.
  Serial.print("C");
  Serial.print( (int)( current_kg * 10 ) );
  Serial.print("M");
  Serial.print( (int)( memory_kg * 10 ) );
  Serial.println("");
}

if(IS_DEBUG && print_counter==0){
  // print the results to the Serial Monitor:
  Serial.print("sensor = ");
  Serial.print(sensor_val);
  Serial.print("\t filter = ");
  Serial.print(filter_val);
  Serial.print("\t diff = ");
  Serial.print(dif_val);
  Serial.print("   \t state = ");
  Serial.print(state);
  Serial.print("\t state_counter = ");
  Serial.print(state_counter);
  Serial.print("\t memory = ");
  Serial.print(memory_val);
  Serial.print("\t memory kg = ");
  Serial.print(memory_kg);
  Serial.print("   \t current kg = ");
  Serial.print(current_kg);
  Serial.println("");
}

if(!IS_DEBUG && Serial.available() ){
  // this section receive command and correct conversion formula.
  
  if(0){
    // zero offset
    zero_offset_val = filter_val;
  }

  if(0){
    // 57.8 is sample commanded value TODO: implement command
    val_to_gram = 57.8 / (float)( zero_offset_val - memory_val );
  }

}
  
  // wait 2 milliseconds before the next loop for the analog-to-digital
  // converter to settle after the last reading:
  delay(2);
}
