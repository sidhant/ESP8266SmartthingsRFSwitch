# ESP8266 + Websockets + Webserver + RC-Switch 433 Mhz + SmartThings
I put this quick and dirty prrof of concept together to familiarize myself with the ESP8266 platform and add it to my arsenal of embedded tools.

### Overview
ESP runs a websocket server and a simple webserver. The webserver exposes a simple API that takes one GET parameter, the RF code and emits that using 433 MHz modules (I used [these]). To learn/sniff the codes, you can visit http://esp8266rc/learn that makes use of a simple HTML page + Websockets (hence the websockets server) to in real-time display sniffed codes. 

As a bonus, the HTTP GET request to send a RF code, thereby turning the switch on/off can be done from the Smartthings hub. This means, I have cetral control of these cheap RF switches and can make them part of my routines, automations and other scripts.

### Parts List
1. 1X [ESP8266]
2. 1X [433 Mhz Pair]
3. Couple Dupont Jumper wires
4. Breadboard

### Usage

1. `WebsocketRC.ino` is flashed onto ESP8266. I am using [this] NodeMCU v1.0.
2. `esp8266_rcSwitch.groovy` is the custom device handler on Smartthings.
3. `http://<esp_ip_address>/learn` to sniff RF codes
4. `http://<esp_ip_address>/switch?code=<code>` to manually send a RF code

### Pin Mapping
```
ESP8266NodeMCU |    433MHz RX
D6            <->   DOUT
```
```
ESP8266NodeMCU |    433MHz TX
D1            <->   DATA
```
VCC/GND as appropriate. I connected all modules to VIN (5V).

[//]: # (These are reference links used in the body of this note and get stripped out when the markdown processor does its job. There is no need to format nicely because it shouldn't be seen. Thanks SO - http://stackoverflow.com/questions/4823468/store-comments-in-markdown-syntax)


   [this]: <http://amzn.to/2haH6Di>
   [these]: <http://amzn.to/2haL9zm>
[433 Mhz Pair]: <http://amzn.to/2haL9zm>
[ESP8266]:  <http://amzn.to/2haH6Di>
