/**
 *  Garage Door Monitor
 *
 *  Author: Zach Naimon c/o SmartThings
 */
 
preferences {
	section("Pick your garage door sensor.") {
		input "garagedoor", "device.smartSenseMulti", title: "Where?"
	}
	section("Time Window") {
		input "timeWindowChoice", "enum", title: "Do you want to enable a time window?", metadata: [values: ["Yes","No"]]
		input "timeWindowStart", "time", title: "Beginning of time window", required: false
		input "timeWindowEnd", "time", title: "End of time window", required: false
		
	}
	section("Secondary Door"){
		input "secondaryDoorChoice", "enum", title: "Do you have a secondary door that you want to add to the system?", metadata: [values: ["Yes","No"]]
		input "secondaryDoor", "capability.contactSensor", title: "Please choose the sensor on your secondary door.", required: false
		
	}
	section("Motion Detector"){
		input "motionSensorChoice", "enum", title: "Do you have a motion that you want to add to the system?", metadata: [values: ["Yes","No"]]
		input "motionSensor", "capability.motionSensor", title: "Please choose the motion sensor installed in your garage.", required: false
	}
	
	section("Maximum Allowable Opening Time for the Garage") {
		input "maxOpenTime", "number", title: "Minutes?"
	}
	section("Text me at...") {
		input "phoneChoice", "enum", title: "Do you want to receive SMS or Push Notifications?", metadata: [values: ["SMS", "Push Notifications"]]
		input "notifFreqChoice", "enum", title: "Do you want to receive multiple messages/notifications?", metadata: [values: ["Yes, Spam Me!", "No! Leave me alone!"]]
		input "notifFreq", "decimal", title: "What frequency (in minutes) do you want messages to arrive?", required: false
		input "phone", "phone", title: "Phone number", required: false
	}
}

def installed(){
	preinit()
}

def updated(){
	unsubscribe()
	preinit()
}

def preinit(){
	if(timeWindowChoice == "Yes" && !timeWindowStart){
			sendPush("If you want a time window, please fill in all the corresponding values, else choose 'No' for this option")
	}
	if(timeWindowChoice == "Yes" && !timeWindowEnd){
		sendPush("If you want a time window, please fill in all the corresponding values, else choose 'No' for this option")
	}
	if(secondaryDoorChoice == "Yes" && !secondaryDoor){
		sendPush("If you want a secondary door on the system, please choose the door sensor, else choose 'No' for this option")
	}
	if(motionSensorChoice == "Yes" && !motionSensor){
		sendPush("If you want to add a motion sensor to the system, please choose the motion sensor, else choose 'No' for this option")
	}
	if(phoneChoice == "SMS" && !phone){
		sendPush("If you want to use SMS for notifications, please enter your phone number, else choose Push Notifications!")
	}
	if(notifFreqChoice == "Yes, Spam Me!" && !notifFreq){
		sendPush("If you want to get spammed, please give us a frequency by filling in that field.")
	}
	else{
		init()
	}
}

def init(){
	if(timeWindowChoice == "Yes" && secondaryDoorChoice == "No" && motionSensorChoice == "No"){
		subscribe(multisensor, "acceleration", accelerationHandler1, [filterEvents: false])
	}
	if(secondaryDoorChoice == "Yes" && timeWindowChoice == "No" && motionSensorChoice == "No"){
		subscribe(multisensor, "acceleration", accelerationHandler2, [filterEvents: false])
	}
	if(motionSensorChoice  == "Yes" && timeWindowChoice == "No" && secondaryDoorChoice == "No"){
		subscribe(multisensor, "acceleration", accelerationHandler3, [filterEvents: false])
	}
	if(timeWindowChoice == "Yes" && secondaryDoorChoice == "Yes" && motionSensorChoice == "No"){
		subscribe(multisensor, "acceleration", accelerationHandler4, [filterEvents: false])
	}
	if(timeWindowChoice == "Yes" && motionSensorChoice == "Yes" && secondaryDoorChoice == "No"){
		subscribe(multisensor, "acceleration", accelerationHandler5, [filterEvents: false])
	}
	if(secondaryDoorChoice == "Yes" && motionSensorChoice == "Yes" && timeWindowChoice == "No"){
		subscribe(multisensor, "acceleration", accelerationHandler6, [filterEvents: false])
	}
	else if(timeWindowChoice == "Yes" && motionSensorChoice == "Yes" && secondaryDoorChoice == "Yes"){
		subscribe(multisensor, "acceleration", accelerationHandler7, [filterEvents: false])
	}
	
}

def accelerationHandler1(evt){
	if(checkOpen() == open && inTimeWindow() == true){
		startTimer()
	}
	else{
		log.debug "Either we're not in the time window or the door hasn't registered as open."
	}
	
}

def accelerationHandler2(evt){
	if(checkOpen() == open && secondaryDoorOpen() == closed){
		startTimer()
	}
	else{
		log.debug "Either the secondary door is open, or the garage door isn't actually open."
	}
}

def accelerationHandler3(evt){
	if(checkOpen() == open && isMotion() == nomotion){
		startTimer()
	}
	else{
		log.debug "Either there's motion, or the garage door isn't actually open."
	}
}

def accelerationHandler4(evt){
	if(checkOpen() == open && inTimeWindow() == true && secondaryDoorOpen() == closed){
		startTimer()
	}
	else{
		log.debug "Either we're not in the time window, or the secondary door is open, or the garage door isn't actually open."
	}	
}

def accelerationHandler5(evt){
	if(checkOpen() == open && inTimeWindow() == true && isMotion() == nomotion){
		startTimer()	
	}
	else{
		log.debug "Either we're not in the time window, or there is motion, or the garage door isn't actually open.  Hey..it rhymes..."
	}
}

def accelerationHandler6(evt){
	if(checkOpen() == open && isMotion() == nomotion && secondaryDoorOpen() == closed){
		startTimer()
	}
	else{
		log.debug "Either the secondary door is open, or there's motion, or the garage door isn't actually open."
	}
}

def accelerationHandler7(evt){
	if(checkOpen() == open && inTimeWindow() == true && secondaryDoorOpen() == closed && isMotion() == nomotion){
		startTimer()
	}
	else{
		log.debug "One of the many variables isn't in line.  We don't need to start the timer."
	}
}

def startTimer(){
	state.timerStart = now()
	def freq = 1
	schedule("0 0/$freq * * * ?", checkTime)	
}
def checkTime(){
	def difTime = now() - state.timerStart
	def maxTime = maxOpenTime * 60000
	if(difTime >= maxTime){
		sendNotif()	
	}
	else{
		log.debug "We're still within the time window.  $difTime is less than $maxTime"
	}
}

def sendNotif(){
	if(phoneChoice == "SMS"){
		if(notifFreqChoice == "Yes, Spam Me!"){
			sendTextMessage()
			schedule("0 0/$notifFreq * * * ?", sendTextMessage)
		}
		else{
			sendTextMessage()
		}
	}
	else if(phoneChoice == "Push Notifications"){
		if(notifFreqChoice == "Yes, Spam Me!"){
			sendPushNotification()
			schedule("0 0/$notifFreq * * *?", sendPushNotification)
		}
		else{
			sendPushNotification()
		}
	}
}

def sendTextMessage(){
	sendSms(phone, "Your garage door is open.  Please advise.")
}

def sendPushNotification(){
	sendPush("Your garage door is open.  Please advise.")
}

private checkOpen(){
	def result
	def latestThreeAxisState = multisensor.threeAxisState // e.g.: 0,0,-1000

	def latestThreeAxisDate = latestThreeAxisState.dateCreated.toSystemDate()
	def isOpen = Math.abs(latestThreeAxisState.xyzValue.z) > 250 // TODO: Test that this value works in most cases...
	if(isOpen){
		return open	
	}
	else{
		return closed	
	}
	result
}


private inTimeWindow(){
	def result
	def now = now()
	def tz = timeZone
	if(now > timeToday(timeWindowStart, tz).time && now < timeToday(timeWindowEnd, tz).time){
		return true
	}
	else{
		return false
	}
	result
}

private secondaryDoorOpen(){
	def result
	if(secondaryDoor.currentValue("contact") == "open"){
		return open
	}
	else if(secondaryDoor.currentValue("contact") == "closed"){
		return closed
	}
	result	
}

private isMotion(){
	def result
	if(motionSensor.currentValue("motion") == "active"){
		return motion
	}
	else if(motionSensor.currentValue("motion") == "inactive"){
		return nomotion
	}
	result
}
