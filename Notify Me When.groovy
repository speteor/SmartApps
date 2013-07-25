/**
 *  Notify Me When
 *
 *  Author: SmartThings
 *  Date: 2013-03-20
 */
preferences {
	section("Choose one or more, when..."){
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
		input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
		input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
   	}
	section("Then send this message in a push notification"){
		input "messageText", "text", title: "Message Text"
	}
	section("And as text message to this number (optional)"){
		input "phone", "phone", title: "Phone Number", required: false
        input "frequency", "decimal", title: "Frequency of messages (in minutes, this is optional)", required: false 
	}

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(contact, "contact.open", sendMessage)
	subscribe(acceleration, "acceleration.active", sendMessage)
	subscribe(motion, "motion.active", sendMessage)
	subscribe(mySwitch, "switch.on", sendMessage)
	subscribe(arrivalPresence, "presence.present", sendMessage)
	subscribe(departurePresence, "presence.not present", sendMessage)
}

def sendMessage(evt) {
	if(frequency){
    	calcTime()
		log.debug "$evt.name: $evt.value, $messageText"
    }
    else if(!frequency){
    	sendNotification()
    }
}

def calcTime(){
	if(state.messageSent){
    	def difTime = now() - state.startTime
		def freqmin = frequency * 60000
		if(difTime >= freqmin){
			sendNotification()
		}
    }
    else{
		sendNotification()
   }
}

def sendNotification(){
	if (phone) {
		sendSms(phone, messageText)
	}
	else{
	    sendPush(messageText)	
	}
	if(frequency){
		state.startTime = now()
		state.messageSent = 1
	}
}
	