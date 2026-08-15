/*
 * IR LED DC TEST — no IRremote, no 38kHz, no timers.
 *
 * Drives D3 HIGH for 3s, LOW for 1s, forever. This turns the IR LED on as a
 * plain DC lamp, which does two things:
 *   1. A camera shows a STEADY violet glow (far easier to spot than a 67ms burst)
 *   2. Gives you a stable 3-second window to probe with a multimeter
 *
 * ~35mA DC is well inside a 5mm 940nm LED's rating, so this is safe to leave running.
 *
 * Expected readings while D3 is HIGH (black probe on GND):
 *   5V rail on shield ........ 4.9 - 5.1 V
 *   D3 pin ................... ~5.0 V
 *   Q1 base .................. ~0.7 V
 *   Q1 collector (LED -) ..... 0.1 - 0.3 V   (transistor saturated = ON)
 *   across R1 (100R) ......... ~3.5 V        (= ~35mA flowing)
 *   across LED (+ to -) ...... 1.2 - 1.4 V
 */

#define IR_SEND_PIN 3

void setup() {
  Serial.begin(115200);
  pinMode(IR_SEND_PIN, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.println(F("DC test: D3 HIGH 3s / LOW 1s. Watch through a phone camera."));
}

void loop() {
  digitalWrite(IR_SEND_PIN, HIGH);
  digitalWrite(LED_BUILTIN, HIGH);
  Serial.println(F("D3 HIGH  <- probe now, LED should glow on camera"));
  delay(3000);

  digitalWrite(IR_SEND_PIN, LOW);
  digitalWrite(LED_BUILTIN, LOW);
  Serial.println(F("D3 LOW"));
  delay(1000);
}
