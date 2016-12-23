#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <WebSocketsServer.h> // https://github.com/Links2004/arduinoWebSockets
#include <ESP8266mDNS.h>
#include <Hash.h>
#include <RCSwitch.h> // https://github.com/sui77/rc-switch/
#include <ctype.h>

#define TX_PIN  5   //GPIO5 on ESP
#define RX_PIN  12  //GPIO12 on ESP

const char *ssid = "SSID";
const char *password = "PASSWORD";

bool RF_RX_ON = false;
uint8_t count = 0;


WebSocketsServer webSocket = WebSocketsServer(81);
RCSwitch mySwitch = RCSwitch();
ESP8266WebServer server(80);

#define USE_SERIAL Serial


String RFlearningPage = "<!DOCTYPE html>\r\n  <meta charset=\"utf-8\" />\r\n  <title>WebSocket Test</title>\r\n  <script language=\"javascript\" type=\"text/javascript\">\r\n\r\n  var wsUri = \"ws://10.0.0.249:81/\";\r\n  var output;\r\n\r\n  function init()\r\n  {\r\n    output = document.getElementById(\"output\");\r\n    testWebSocket();\r\n  }\r\n\r\n  function testWebSocket()\r\n  {\r\n    websocket = new WebSocket(wsUri);\r\n    websocket.onopen = function(evt) { onOpen(evt) };\r\n    websocket.onclose = function(evt) { onClose(evt) };\r\n    websocket.onmessage = function(evt) { onMessage(evt) };\r\n    websocket.onerror = function(evt) { onError(evt) };\r\n  }\r\n\r\n  function onOpen(evt)\r\n  {\r\n    writeToScreen(\"CONNECTED\");\r\n    doSend(\"Ping\");\r\n  }\r\n\r\n  function onClose(evt)\r\n  {\r\n    writeToScreen(\"DISCONNECTED\");\r\n  }\r\n\r\n  function onMessage(evt)\r\n  {\r\n    writeToScreen('<span style=\"color: blue;\">RESPONSE: ' + evt.data+'</span>');\r\n    \r\n  }\r\n\r\n  function onError(evt)\r\n  {\r\n    writeToScreen('<span style=\"color: red;\">ERROR:</span> ' + evt.data);\r\n  }\r\n\r\n  function doSend(message)\r\n  {\r\n    writeToScreen(\"SENT: \" + message);\r\n    websocket.send(message);\r\n  }\r\n\r\n  function writeToScreen(message)\r\n  {\r\n    var pre = document.createElement(\"p\");\r\n    pre.style.wordWrap = \"break-word\";\r\n    pre.innerHTML = message;\r\n    output.appendChild(pre);\r\n  }\r\n\r\n  function close()\r\n  {\r\n    websocket.close();\r\n  }\r\n\r\n  window.addEventListener(\"load\", init, false);\r\n\r\n  </script>\r\n\r\n  <h2>ESP8266 RC Listener Interface</h2>\r\n  <input id=\"clickMe\" type=\"button\" value=\"RF Listening ON\" onclick=\"doSend('RF_RX_ON');\" />\r\n  <input id=\"clickMe\" type=\"button\" value=\"RF Listening OFF\" onclick=\"doSend('RF_RX_OFF');\" />\r\n\r\n\r\n  <div id=\"output\"></div>\r\n";


void webSocketEvent(uint8_t num, WStype_t type, uint8_t * payload, size_t lenght) {

    switch(type) {
        case WStype_DISCONNECTED:
            USE_SERIAL.printf("[%u] Disconnected!\n", num);
            break;
        case WStype_CONNECTED:
            {
                IPAddress ip = webSocket.remoteIP(num);
                USE_SERIAL.printf("[%u] Connected from %d.%d.%d.%d url: %s\n", num, ip[0], ip[1], ip[2], ip[3], payload);
        
                // send message to client
                webSocket.sendTXT(num, "Connected");
            }
            break;
        case WStype_TEXT:
            {
              USE_SERIAL.printf("[%u] get Text: %s\n", num, payload);
              String text = String((char *) &payload[0]);
  
              if(text.startsWith("RF_RX_ON"))
              {
                //Enable Receive
                mySwitch.enableReceive(RX_PIN);
                RF_RX_ON = true;
                webSocket.sendTXT(num, "Turning ON RF Sniffing");
              }
              
              if(text.startsWith("RF_RX_OFF"))
              {
                mySwitch.disableReceive();
                RF_RX_ON = false;
                webSocket.sendTXT(num, "Turning OFF RF Sniffing");
              }
               
              break;
           }
        case WStype_BIN:
            USE_SERIAL.printf("[%u] get binary lenght: %u\n", num, lenght);
            hexdump(payload, lenght);

            // send message to client
            // webSocket.sendBIN(num, payload, lenght);
            break;
    }

}

boolean isValidNumber(String str){
   for(byte i=0;i<str.length();i++)
    {
      if(!isDigit(str.charAt(i))) 
      {
        return false;
      }
    }

   return true;
} 

void handleRoot() {
  server.send(200, "text/plain", "hello from esp8266!");
}

void handleRFLearning() {
  server.send(200, "text/html", RFlearningPage);
}

void handleNotFound(){

  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET)?"GET":"POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";
  for (uint8_t i=0; i<server.args(); i++){
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }
  server.send(404, "text/plain", message);
  USE_SERIAL.println("Request: " +  server.uri());
}

void handleSwitch()
{
  String RFcode = server.arg("code");
  if(isValidNumber(RFcode))
  {
    int code = RFcode.toInt();
    mySwitch.send(code, 24);
    server.send(200, "text/plain", "Code Transmitted");
  }
  else
  {
    server.send(13, "text/plain", "Code is not a number");    
  }

  USE_SERIAL.println("Request: " +  server.uri() + " Args: " + server.args());
  
}
void setup() {
        
        USE_SERIAL.begin(115200);
        USE_SERIAL.setDebugOutput(true);

        //Instantiate RC-Switch
        // Transmitter is connected to Arduino Pin #10  
        mySwitch.enableTransmit(TX_PIN);
        // Optional set pulse length -- found to be ~182 for Etekcity
        mySwitch.setPulseLength(182);
        
        WiFi.begin ( ssid, password );
        // Wait for connection
        while ( WiFi.status() != WL_CONNECTED ) {
        delay ( 500 );
        Serial.print ( "." );
        }
        
        Serial.println ( "" );
        Serial.print ( "Connected to " );
        Serial.println ( ssid );
        Serial.print ( "IP address: " );
        Serial.println ( WiFi.localIP() );

        if (MDNS.begin("esp8266rc")) {
          Serial.println("MDNS responder started");
        }

        //Register HTTP server callbacks
        server.on("/", handleRoot);
        server.on("/switch", handleSwitch);         //Control RC Switch
        server.on("/learn", handleRFLearning); //Learn codes by sniffing
        server.onNotFound(handleNotFound);

        
        webSocket.begin();
        webSocket.onEvent(webSocketEvent);
        Serial.println("Websocket server started");

        server.begin();
        Serial.println("HTTP server started");
}

void loop() {
   
   //Websocket Requests
   webSocket.loop();
   
   //HTTP Requests
   server.handleClient();
   
   if(RF_RX_ON && mySwitch.available())
   {
      int value = mySwitch.getReceivedValue();
      String str = String("RX: " + String(mySwitch.getReceivedValue()) \
                          + " / " + String(mySwitch.getReceivedBitlength()) \
                          + "bit Proto:" + String(mySwitch.getReceivedProtocol()));
      mySwitch.resetAvailable();
      
      webSocket.broadcastTXT(str);
   }
    
}
