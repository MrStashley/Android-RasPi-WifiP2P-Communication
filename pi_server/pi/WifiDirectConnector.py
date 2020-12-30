import serial, os, sys, threading;
from wifi_direct_socket import WifiDirectSocket

BLE_INIT_FAILED = ord("0");
BLE_INIT_SUCCESS = ord("1");
CENTRAL_CONNECTED = ord("2");
DEVICE_NAME = ord("3");
START_CONNECT = ord("4");
CENTRAL_DISCONNECTED = ord("5");
DISCONNECT_WFD = ord("7");
START_SOCKET = ord("8");


P2P_FIND_COMMAND = "wpa_cli -i p2p-dev-wlan0 p2p_find";
GET_P2P_PEERS_COMMAND = "for i in $( wpa_cli -i p2p-dev-wlan0 p2p_peers ); do echo -n \"$i \n\"; wpa_cli -i p2p-dev-wlan0 p2p_peer $i | grep -E \"device_name=\"; done";
P2P_GROUP_REMOVE_COMMAND = "wpa_cli -ip2p-dev-wlan0 p2p_group_remove  $(ip -br link | grep -Po 'p2p-wlan0-\\d+')" ;

def P2P_CONNECT_COMMAND(macd):
    #python macro B-). idk if this is a thing that is done in standard python style but idc i'm doin it
    return "wpa_cli -i p2p-dev-wlan0 p2p_connect " + macd + " pbc";

class WifiDirectConnector:
    
    def __init__(self):
        self.device_name = "";
        self.connected = False;
        self.main();

    def main(self):
        comPort = "/dev/ttyACM0"; #port that will be used if one is not specified (COM3 on windows)
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
                    command_code = data[1];
                    print("Command registered: " + chr(command_code));
                    value = data[3:];
                    
                    if(command_code == DEVICE_NAME):
                        self.set_device_name(value);
                    if(command_code == CENTRAL_CONNECTED):
                        self.central_connected();
                    if(command_code == CENTRAL_DISCONNECTED):
                        self.central_disconnected();
                    if(command_code == START_CONNECT):
                        self.start_connect();
                    if(command_code == DISCONNECT_WFD):
                        self.disconnect();
                    if(command_code == START_SOCKET):
                        self.wifi_direct_connected();
                        
                print(data);


    def disconnect(self):
        self.connected = False;
        print("disconnecting");

        if (not self.p2p_group_remove()):
            print("Disconnect failed");
        else:
            print("Disconnected");
                
    def start_connect(self):
        if(self.device_name == ""):
            return;
        
        print("Starting connection with " + str(self.device_name));

        if(not self.p2p_find()):
            print("p2p_find failed. stopping");
            return;

        macd = self.get_mac_address();

        print("got macd: " + str(macd));

        if(self.p2p_connect(macd)):
            print("successfully connected to " + str(self.device_name));
            self.connected = True;
            
        else:
            print("Connection failed. Something went wrong");

    def central_connected(self):
        self.connected = False;
        if(not self.p2p_find()):
            print("p2p_find failed. stopping");
            return;
            
    def central_disconnected(self):
        self.device_name = "";
      
    def set_device_name(self, pdevice_name):
        pdevice_name = str(pdevice_name)[2:-1];
        print("Setting device name: " + str(pdevice_name));
        self.device_name = pdevice_name;


    def p2p_find(self):
        to_return = False;
        p2p_find_file = os.popen(P2P_FIND_COMMAND, 'r');
        cmd_output = p2p_find_file.readline();

        if(cmd_output == "OK\n"):
            to_return = True;
            
        p2p_find_file.close();

        return to_return;

    def get_mac_address(self):
        to_return = "";

        get_peers_file = os.popen(GET_P2P_PEERS_COMMAND, 'r');
        while(True):
            macd = get_peers_file.readline()[:-1];
            device_name = get_peers_file.readline()[12:-1];
            
            if(macd == "" or device_name == ""):
                break;
            
            if(device_name == self.device_name):
                to_return  = macd;
                break;

        get_peers_file.close();
        
        return to_return;

    def p2p_connect(self, macd):
        to_return = False;
        
        p2p_connect_file = os.popen(P2P_CONNECT_COMMAND(macd), 'r');
        cmd_output = p2p_connect_file.readline();

        if(cmd_output == "OK\n"):
            to_return = True;

        return to_return;

    def p2p_group_remove(self):
        to_return = False;

        p2p_group_remove_file = os.popen(P2P_GROUP_REMOVE_COMMAND, "r");
        cmd_output = p2p_group_remove_file.readline();

        if(cmd_output == "OK\n"):
            to_return = True;

        return to_return;

    def wifi_direct_connected(self):
        self.socket = WifiDirectSocket();
        self.socket.create_socket();
        self.socket.accept();
  
if __name__ == "__main__":
    wifi_direct_conn = WifiDirectConnector();
