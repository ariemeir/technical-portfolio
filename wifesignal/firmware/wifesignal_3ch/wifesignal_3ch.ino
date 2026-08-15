/*
 * WifeSignal - 3-channel BLE firmware (Rev A PCB)
 * Board:  ESP32-C3 SuperMini Plus (ESP32C3 Dev Module, USB CDC On Boot = Enabled)
 * BLE:    NimBLE-Arduino v2.x
 *
 * One STATE byte drives three illuminated arcade buttons:
 *   0 = all off, 1 = red, 2 = yellow, 3 = green  (single active light)
 *
 * Pins (as built):
 *   switches  RED=GPIO3  YEL=GPIO4  GRN=GPIO5   (INPUT_PULLUP, press = LOW, COM -> GND)
 *   lamps     RED=GPIO6  YEL=GPIO7  GRN=GPIO10
 *   onboard LED GPIO8 (INVERTED, LOW = ON) mirrors state != 0
 *
 * LAMP POLARITY: drivers are S8550 PNP (not the S8050 NPN on the schematic -
 * assortment-kit mixup, the soldered parts win). GPIO LOW = lamp ON,
 * HIGH = lamp OFF. Do not "fix" this to NPN logic.
 *
 * Button semantics (must match server.py's echo-suppression logic):
 *   press any button while state != 0 -> state = 0 (physical acknowledge)
 *   press a button while state == 0   -> state = that button's color
 *     (the server writes it back to 0; reserved "ping back" hook)
 * A BLE central can WRITE (0-3), READ, and gets NOTIFY on every change.
 */

#include <NimBLEDevice.h>

#define SVC_UUID   "6d5f0001-4b6b-4a3a-9e1e-2a7b1c9f0001"
#define STATE_UUID "6d5f0002-4b6b-4a3a-9e1e-2a7b1c9f0002"

const int NCH = 3;
const int BTN[NCH]  = {3, 4, 5};    // red, yellow, green
const int LAMP[NCH] = {6, 7, 10};   // red, yellow, green (LOW = ON, PNP)
const int LEDB = 8;                 // onboard LED, INVERTED (LOW = ON)

NimBLECharacteristic* stateChar = nullptr;
uint8_t state = 0;                  // 0=off, 1=red, 2=yellow, 3=green
bool connected = false;

// debounce, per button
int lastRead[NCH] = {HIGH, HIGH, HIGH};
uint32_t lastEdge[NCH] = {0, 0, 0};
bool handled[NCH] = {false, false, false};

void applyLamps() {
  for (int i = 0; i < NCH; i++)
    digitalWrite(LAMP[i], (state == i + 1) ? LOW : HIGH);   // PNP: LOW = ON
  digitalWrite(LEDB, state ? LOW : HIGH);                   // onboard: inverted
}

void pushState(bool doNotify) {
  stateChar->setValue(&state, 1);
  if (doNotify && connected) stateChar->notify();
}

class StateCallbacks : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic* c, NimBLEConnInfo& info) override {
    std::string v = c->getValue();
    if (v.empty()) return;
    state = (v[0] <= 3) ? v[0] : 0;
    applyLamps();
    pushState(true);
    Serial.printf("write -> state %d\n", state);
  }
};

class ServerCallbacks : public NimBLEServerCallbacks {
  void onConnect(NimBLEServer* s, NimBLEConnInfo& i) override {
    connected = true; Serial.println("central connected");
  }
  void onDisconnect(NimBLEServer* s, NimBLEConnInfo& i, int reason) override {
    connected = false; Serial.println("central disconnected");
    NimBLEDevice::startAdvertising();
  }
};

void setup() {
  // Lamps OFF (HIGH) before anything else - latch the level, then enable output
  for (int i = 0; i < NCH; i++) {
    digitalWrite(LAMP[i], HIGH);
    pinMode(LAMP[i], OUTPUT);
    digitalWrite(LAMP[i], HIGH);
  }
  digitalWrite(LEDB, HIGH);
  pinMode(LEDB, OUTPUT);
  digitalWrite(LEDB, HIGH);
  for (int i = 0; i < NCH; i++) pinMode(BTN[i], INPUT_PULLUP);

  Serial.begin(115200);
  applyLamps();

  Serial.printf("pins -> BTN=%d/%d/%d  LAMP=%d/%d/%d (R/Y/G)  LEDB=%d\n",
                BTN[0], BTN[1], BTN[2], LAMP[0], LAMP[1], LAMP[2], LEDB);

  NimBLEDevice::init("WifeSignal");
  NimBLEDevice::setPower(ESP_PWR_LVL_P9);

  NimBLEServer* server = NimBLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  NimBLEService* svc = server->createService(SVC_UUID);
  stateChar = svc->createCharacteristic(STATE_UUID,
      NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE | NIMBLE_PROPERTY::NOTIFY);
  stateChar->setCallbacks(new StateCallbacks());
  pushState(false);
  svc->start();

  NimBLEAdvertising* adv = NimBLEDevice::getAdvertising();
  adv->addServiceUUID(SVC_UUID);
  adv->setMinInterval(160);   // 100 ms
  adv->setMaxInterval(320);   // 200 ms
  adv->setName("WifeSignal");
  adv->start();
  Serial.println("BLE up - advertising as WifeSignal");
}

void loop() {
  uint32_t now = millis();
  for (int i = 0; i < NCH; i++) {
    int r = digitalRead(BTN[i]);
    if (r != lastRead[i]) { lastRead[i] = r; lastEdge[i] = now; }
    if (now - lastEdge[i] > 40) {
      if (r == LOW && !handled[i]) {
        handled[i] = true;
        state = state ? 0 : (uint8_t)(i + 1);
        applyLamps();
        pushState(true);
        Serial.printf("press btn %d -> state %d\n", i + 1, state);
      } else if (r == HIGH) {
        handled[i] = false;
      }
    }
  }
  delay(5);
}
