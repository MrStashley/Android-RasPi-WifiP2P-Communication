#include <ArduinoBLE.h>
#define MAX_BLE_CHAR_LENGTH 30

#define BLE_INIT_FAILED "_0:"
#define BLE_INIT_SUCCESS "_1:"
#define CENTRAL_CONNECTED "_2:"
#define DEVICE_NAME "_3:"
#define START_CONNECT "_4:"
#define CENTRAL_DISCONNECTED "_5:"

BLEService WifiDirectConn("19B10010-E8F2-537E-4F6C-D104768A1214");

char device_name_buffer[MAX_BLE_CHAR_LENGTH];
BLECharacteristic DeviceNameChar("1101", BLEWrite, '0', MAX_BLE_CHAR_LENGTH);

char start_connection[1];
BLEBooleanCharacteristic StartConnectionChar("1102", BLEWrite);




void setup() {
  pinMode(LED_BUILTIN, OUTPUT);

  Serial.begin(9600);
  if(!BLE.begin()){
    Serial.println(BLE_INIT_FAILED);
    while(1); // think about what defensive programming to do here
  }

  BLE.setLocalName("SILQ_1");
  BLE.setAdvertisedServiceUuid(WifiDirectConn.uuid());
  
  WifiDirectConn.addCharacteristic(DeviceNameChar);
  WifiDirectConn.addCharacteristic(StartConnectionChar);
  
  BLE.addService(WifiDirectConn);
  BLE.advertise();

  device_name_buffer[0] = '\0';

  Serial.println(BLE_INIT_SUCCESS);

  digitalWrite(LED_BUILTIN, LOW);

}

void loop() {
  BLEDevice central = BLE.central();

  if(central){
    Serial.print(CENTRAL_CONNECTED);
    Serial.println(central.address());

    while(central.connected()){
  
      DeviceNameChar.readValue(device_name_buffer, MAX_BLE_CHAR_LENGTH);
      if(device_name_buffer[0] != '\0'){
        Serial.print(DEVICE_NAME);
        Serial.println(device_name_buffer);
        device_name_buffer[0] = '\0';
        DeviceNameChar.writeValue(device_name_buffer, MAX_BLE_CHAR_LENGTH);
      }
  
      StartConnectionChar.readValue(start_connection, 1);
      if((int)start_connection[0]){
        Serial.println(START_CONNECT);
        StartConnectionChar.writeValue(0x00);
      }
    }
    Serial.println(CENTRAL_DISCONNECTED);
  }

}
