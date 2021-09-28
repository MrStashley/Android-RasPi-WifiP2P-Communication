This is the code running on the Pi that initializes the connection and sends data.

Implementation: 

I am using a raspberry pi zero w connected serially to an Arduino Nano 33 BLE sense.
The Connection is first initiated through Bluetooth Low Energy. The devices exchange device names and the android is able to send a 
signal to the pi telling it to start the wifi direct connection.

WifiDirectConnector_py.py is the pi code and bluetooth_handler_arduino.ino is the arduino code

Alternate implementations: <br> 
Any raspberry pi that supports wifi direct and any BLE capable arduino or programmable embedded device would probably work here, no promises though.
I chose to sideload the BLE onto the arduino because it's simpler and I require an arduino for my project anyway, but if you would prefer to use the raspberry pi's BLE or bluetooth, you can implement it
and just call it in the main method of the WifiDirectConnector python object, calling the functions that set device name and initiate connection and such.
I am going to add support for GPIO instead of serial at a later date
