/**
 *  Flood Alert
 *
 *  Author: SmartThings
 */
preferences {
	section("When there's water detected...") {
		input "alarm", "capability.waterSensor", title: "Where?"
	}
	section("Text me at...") {
		input "phone", "phone", title: "Phone number?", required: false
        input "frequency", "decimal", title: "Frequency of Alerts (in minutes)", required: false;
	}
}

def installed() {
	subscribe(alarm, "water.wet", waterWetHandler)
}

def updated() {
	unsubscribe()
	subscribe(alarm, "water.wet", waterWetHandler)
}

def waterWetHandler(evt) {
	if(frequency){
    	calcTime()
    }
    else{
    	sendMessage()
    }	
}

def calcTime(){
	def now = now()
	def freq = frequency * 60000
	if(state.startTime){
		def difTime = now - state.startTime
		if(difTime >= freq){
			sendMessage()			
		}
	}
	else{
		sendMessage()
	}
}

def sendMessage(){
	def msg = "${alarm.displayName} is wet!"
	log.debug "$alarm is wet, texting $phone"
	sendPush(msg)
	if (phone) {
		sendSms(phone, msg)
	}
	state.startTime = now()
}
