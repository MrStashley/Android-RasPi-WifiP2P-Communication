#this sets up your raspberry pi to communicate with wifi-direct
#warning, this will mess up any network configuration that you may already have
# you can create a normal wifi connection with systemd-networkd, but if you need the features of
# normal raspbian networking services, do not use this


#vars, change them as necessary
country="US"
device_name="DIRECT-RasPi1"
p2p_go_intent=15


echo "Setting up wifi direct capabilities"
echo "This will restart your system at the end of the setup"
echo "use control-c to stop execution if you need to"

#install tools
#these aren't necessary, you can get rid of these 3 lines if you want
sudo apt install nmap
sudo apt install screen
sudo apt install emacs

#deinstall classic networking
apt --autoremove purge ifupdown dhcpcd5 isc-dhcp-client isc-dhcp-common rsyslog;
apt-mark hold ifupdown dhcpcd5 isc-dhcp-client isc-dhcp-common rsyslog raspberrypi-net-mods openresolv;
rm -r /etc/network /etc/dhcp;

# setup/enable systemd-resolved and systemd-networkd
apt --autoremove purge avahi-daemon
apt-mark hold avahi-daemon libnss-mdns
apt install libnss-resolve
ln -sf /run/systemd/resolve/stub-resolv.conf /etc/resolv.conf
systemctl enable systemd-networkd.service systemd-resolved.service

#setup wifi direct

#set /etc/wpa_supplicant/wpa_supplicant-wlan0.conf
cat > /etc/wpa_supplicant/wpa_supplicant-wlan0.conf <<EOF
ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
driver_param=p2p_device=6
update_config=1
device_name=$device_name
#specifies that the device is Network Infrastructure / AP
device_type=6-0050F204-1
#supports 802.11n for the p2p group owner
p2p_go_ht40=1
#0 => not group owner, 7 => 50% chance, 15 => definitely group owner 
p2p_go_intent=$p2p_go_intent
country=$country
#specifies push button connection
config_methods=virtual_push_button
EOF

#enable wpa_supplicant
chmod 600 /etc/wpa_supplicant/wpa_supplicant-wlan0.conf
systemctl disable wpa_supplicant.service
systemctl enable wpa_supplicant@wlan0.service
rfkill unblock wlan

#setup dhcp server so the pi can be group owner
cat > /etc/systemd/networkd/12-p2p-wlan0.network <<EOF
[Match]
name=p2p-wlan0-*
[Network]
Address=192.168.4.1/24
DHCPServer=yes
EOF

echo "the setup is now complete, rebooting your system"
reboot
