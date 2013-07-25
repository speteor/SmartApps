preferences {
	section("These Sensors...") {
		input "presenceSensor", "capability.presenceSensor", title: "Presence Sensor(s)", multiple: true;
		input "motionSensors", "capability.motionSensor", title: "Motion Sensor(s)", multiple: true;
		
	}
	section("Switches"){
		input "lights", "capability.switch", title: "These Switches", multiple: true;
	}
	section("Other Preferences."){
		input "threshold", "decimal", title: "Threshold for turnoff"
		input "startTime", "time", title: "Start  Time for this App everyday."
		// input "enableConverse", "enum", title: "Enable LightsOn if motion is detected"
	}
}

def installed(){
	init()
}

def updated(){
	unsubscribe()
	init()
}

def init(){
	subscribe(presenceSensor, "presence", presence)
    subscribe(motionSensors, "motion", motion)
}

def runJob(){
	
	
	def presenceValue = presenceSensor.find{it.currentPresence == "not present"}
	def motionValue = motionSensors.find{it.currentMotion == "inactive"}
	if(now >= startTime && !presenceValue && motionValue){
		turnOff()
	}
	
	else{
		log.debug "nothing here as of yet."
	}
}

def presence(evt){
	runJob()
}

def motion(evt){
	runJob()
}

def turnOff(){
	def now = now()
    def thresh = threshold as int
    def nowPlus = (now + threshold)
    def timeOff = new Date(nowPlus)
    runOnce(timeOff, lightsOff)
}

def lightsOff(){
	lights.off()
}