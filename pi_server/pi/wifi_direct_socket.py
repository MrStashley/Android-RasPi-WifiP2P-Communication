import socket;

class WifiDirectSocket:
    def __init__(self):
        self.server = None;
        

    def create_socket(self):
        host = "192.168.4.1";
        port = 4444;

        try:
            self.server = socket.socket();
            self.server.bind((host, port));
            self.server.listen();
        except OSError:
            self.create_socket();

    def accept(self):
        client, addr = self.server.accept();
        print (client.recv(2056));

        client.close();
