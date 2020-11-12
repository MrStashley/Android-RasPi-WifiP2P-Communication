import serial, sys, threading;

BLE_INIT_FAILED = ord("0");
BLE_INIT_SUCCESS = ord("1");
CENTRAL_CONNECTED = ord("2");
DEVICE_NAME = ord("3");
START_CONNECT = ord("4");
CENTRAL_DISCONNECTED = ord("5");

class WifiDirectConnector:
    
    def __init__(self):
        self.device_name = "";
        self.main();

    def main(self):
        comPort = "COM3"; #com port that will be used if one is not specified
        baud = 9600; #default baudrate
        if(len(sys.argv) > 1): #accept other com port and baudrate if one is given
            comPort = sys.argv[1];
            if(len(sys.argv) > 2):
                baud = sys.argv[2];
                
                
        serialConn = serial.Serial(comPort,baud, timeout=.1); #creates an object that watches the given port at the given baudrate
        
        while True:
            data = serialConn.readline()[:-2] #reads a line; the last byte is a newline character and we don't need it
            if data: 
                if(data[0] == ord('_')):
                    print("Command registered: " + chr(data[1]));
                    value = data[3:];
                    
                    if(data[1] == DEVICE_NAME):
                        self.set_device_name(value);
                    if(data[1] == CENTRAL_DISCONNECTED):
                        self.central_disconnected();
                    if(data[1] == START_CONNECT):
                        self.start_connect();
                        
                print(data);
                
    def start_connect(self):
        if(self.device_name != ""):
            print("Starting connection with " + str(self.device_name));
                
    def central_disconnected(self):
        self.device_name = "";
      
    def set_device_name(self, pdevice_name):
        print("Setting device name: " + str(pdevice_name));
        self.device_name = pdevice_name;
  
if __name__ == "__main__":
    wifi_direct_conn = WifiDirectConnector();