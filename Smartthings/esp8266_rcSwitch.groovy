/**
 *  Generic HTTP Device v1.0.20160402
 *
 *  Source code can be found here: https://github.com/JZ-SmartThings/SmartThings/blob/master/Devices/Generic%20HTTP%20Device/GenericHTTPDevice.groovy
 *
 *  Copyright 2016 JZ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.JsonSlurper

metadata {
	definition (name: "ESP8266 - RCSwitch", author: "sidhant", namespace:"sidhant") {
		capability "Switch"
		attribute "triggerswitch", "string"
		command "DeviceTrigger"
	}


	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Please enter port 80 or your device's Port", required: true, displayDuringSetup: true)
		input("DevicePath", "string", title:"URL Path", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
        input("HTTPparam", "string", title:"GET/POST Param Name", description: "HTTP param. For myParam=xyz, enter myParam", displayDuringSetup: true)
		input("RF_ON_Code", "string", title:"RF ON Code", description: "RF Code for On, Integer 24bit", required:true, displayDuringSetup: true)
        input("RF_OFF_Code", "string", title:"RF OFF Code", description: "RF Code for Off, Integer 24bit", required:true, displayDuringSetup: true)
        input(name: "DevicePostGet", type: "enum", title: "POST or GET", options: ["POST","GET"], required: true, displayDuringSetup: true)
		input("DeviceBodyText", "string", title:'HTTP Request Body Content', description: 'Type in "GateTrigger=" or "CustomTrigger="', required: false, defaultValue: "", displayDuringSetup: true)
		input("UseJSON", "bool", title:"Use JSON instead of HTML?", description: "Use JSON instead of HTML?", defaultValue: false, required: false, displayDuringSetup: true)
		section() {
			input("HTTPAuth", "bool", title:"Requires User Auth?", description: "Choose if the HTTP requires basic authentication", defaultValue: false, required: true, displayDuringSetup: true)
			input("HTTPUser", "string", title:"HTTP User", description: "Enter your basic username", required: false, displayDuringSetup: true)
			input("HTTPPassword", "string", title:"HTTP Password", description: "Enter your basic password", required: false, displayDuringSetup: true)
		}
	}

	simulator {
	}

	tiles {
		standardTile("DeviceTrigger", "device.triggerswitch", width: 3, height: 3, canChangeIcon: true, canChangeBackground: true) {
			state "triggeroff", label:'OFF' , action: "on", icon: "st.Appliances.appliances17", backgroundColor:"#ffffff", nextState: "trying"
			state "triggeron", label: 'ON', action: "off", icon: "st.Appliances.appliances17", backgroundColor: "#79b821", nextState: "trying"
			state "trying", label: 'TRYING', action: "", icon: "st.Appliances.appliances17", backgroundColor: "#FFAA33"
		}
		main "DeviceTrigger"
		details(["DeviceTrigger", "oscTrigger", "modeTrigger", "speedTrigger", "timerAddTrigger", "timerMinusTrigger"])
	}
}

def on() {
	log.debug "RC Switch Triggered ON"
	//sendEvent(name: "triggerswitch", value: "triggeron", isStateChange: true)
    state.switch = "on";
	runCmd(RF_ON_Code)
}
def off() {
	log.debug "RC Switch Triggered OFF"
	//sendEvent(name: "triggerswitch", value: "triggeroff", isStateChange: true)
    state.switch = "off";
	runCmd(RF_OFF_Code)
}


def runCmd(String varCommand) {
	def host = DeviceIP
	def hosthex = convertIPtoHex(host).toUpperCase()
	def porthex = convertPortToHex(DevicePort).toUpperCase()
	device.deviceNetworkId = "$hosthex:$porthex"
	def userpassascii = "${HTTPUser}:${HTTPPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()

	log.debug "The device id configured is: $device.deviceNetworkId"

	//def path = DevicePath
	def path = DevicePath + "?" + HTTPparam + "=" + varCommand
	log.debug "path is: $path"
	log.debug "Uses which method: $DevicePostGet"
	def body = DeviceBodyText
	log.debug "body is: $body"

	def headers = [:]
	headers.put("HOST", "$host:$DevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	if (HTTPAuth) {
		headers.put("Authorization", userpass)
	}
	log.debug "The Header is $headers"
	def method = "GET"
	try {
		if (DevicePostGet.toUpperCase() == "GET") {
			method = "GET"
			}
		}
	catch (Exception e) {
		settings.DevicePostGet = "POST"
		log.debug e
		log.debug "You must not have set the preference for the DevicePOSTGET option"
	}
	log.debug "The method is $method"
	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		hubAction.options = [outputMsgToS3:false]
		//log.debug hubAction
		hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
    
    def msg = parseLanMessage(description)
    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)
    
    //log.debug "Return Code: " + status + " body: " + body
    
	def whichTile = ''	
	log.debug "state.switch " + state.switch
	if(status == 200)
    {
        log.debug "Code 200. OK. Changing switch state"
        
        if (state.switch == "on") {
            sendEvent(name: "triggerswitch", value: "triggeron", isStateChange: true)
            whichTile = 'mainon'
        }
        if (state.switch == "off") {
            sendEvent(name: "triggerswitch", value: "triggeroff", isStateChange: true)
            whichTile = 'mainoff'
        }

        //RETURN BUTTONS TO CORRECT STATE
        log.debug 'whichTile: ' + whichTile
        switch (whichTile) {
            case 'mainon':
                def result = createEvent(name: "switch", value: "on", isStateChange: true)
                return result
            case 'mainoff':
                def result = createEvent(name: "switch", value: "off", isStateChange: true)
                return result
            default:
                def result = createEvent(name: "testswitch", value: "default", isStateChange: true)
                //log.debug "testswitch returned ${result?.descriptionText}"
                return result
        }
    
    }
    else // ESP8266 side of things sends a code 13 when code param is malformed
    {
    	log.debug "Code NOT OK. Not changing switch state. Code:" + status 
        
        //Revert states
        if(state.switch == "on")
        	state.switch = "off";
        else if(state.switch == "off")
        	state.switch = "on";
    }
    
}

private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	//log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
	return hex
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	//log.debug hexport
	return hexport
}
private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	//log.debug("Convert hex to ip: $hex")
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
	//log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}