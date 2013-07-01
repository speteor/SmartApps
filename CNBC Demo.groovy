/**
 *  TV Time
 *
 *  Author: Zach Naimon
 *			zach@beamlee.com
 *  Date: 2013-06-18
 */
preferences {
	section("Please set a main allowable time window") {
		input "startTime1", "time", title: "Start of main time window"
		input "endTime1", "time", title: "End of main time window"
	}
	section("Please set a secondary allowable time window (Optional)") {
		input "startTime2", "time", title: "Start of secondary time window", required: false
		input "endTime2", "time", title: "End of secondary time window", required: false
	}
	section("TV Time"){
		input "maxTimeWk", "decimal", title: "Maximum TV time on weekdays (hrs)", required: false
		input "maxTimeWe", "decimal", title: "Maximum TV time on weekends (hrs)", required: false
	}
	section("TV(s)"){
		input "tv", "device.zwaveMeteringSwitch", title: "Select your television(s)", multiple: true
	}
	section("Contacting You"){
		input "phone", "phone", title: "Enter a phone number", required: false
	}
}

def installed() {
	subscribe(tv, "power", tvHandler)
    
	schedule("0 * * * * ?", checkTime)
    state.totalTime = 0
}

def updated() {
	unsubscribe()
	subscribe(tv, "power", tvHandler)
    schedule("0 * * * * ?", checkTime)
    state.totalTime = 0
	
}
def checkSwitch(){
	tv.poll()
}

def checkTime(){
	if((startTime2) && (endTime2)){
		if(now() >= timeToday(startTime2).time && now() < timeToday(endTime2).time){
			tv.on()
			schedule("0 * * * * ?", checkSwitch)
        }
    }
    if(now() >= timeToday(startTime1).time && now() < timeToday(endTime1).time){
    		tv.on()
			schedule("0 * * * * ?", checkSwitch)
    }
    else{
    		tv.off()
    }
}

def getQuotaWk(){
	 maxTimeWk * 3600000
 }
def getQuotaWe(){
	maxTimeWe * 3600000
}

def calcDay(){
	state.caltime = new Date().format("hh:mm")
    log.debug "Time is currently $state.caltime"
	
}

def startTimer(){
	state.startT = now()
    log.debug "Started Timer at $state.startT"
	def freq = 1
	   
}

def stopTimer(){
  	def endT = now()
    log.debug "Ended Timer at $endT"
    state.thisTime = endT - state.startT
    log.debug "This instance of timer recorded $state.thisTime"
    calcTime()
    
}

def calcTime(){
	def freq = 1
	
    calcDay()
    if(state.caltime == "00:00"){	
    	log.debug "Total time today: $totalTime"
    	state.totalTime = 0
        state.origTime = null
        log.debug "Midnight reached.  Resetting timer"
        
	}
    else{
		if(state.totalTime){
    		state.totalTime = state.totalTime + state.thisTime
    	} 
   		else{
   			state.totalTime = state.thisTime
   		}
   		log.debug "Total time so far is $state.totalTime"
	 }
}

def sendNotifGood(){
	def weekend = ["Sat","Sun"].contains(new Date().format("EEE"))
	if(weekend == true){
		if(phone){
			sendSms(phone, "TV is On.  TV was turned on within set time parameters and your quota of $maxTimeWe hours has not been reached.")
		}
		else{
			sendPush("TV is On.  TV was turned on within set time parameters and your quota of $maxTimeWe hours has not been reached.")
		}
	}
	else{
		if(phone){
			sendSms(phone, "TV is On.  TV was turned on within set time parameters and your quota of $maxTimeWk hours has not been reached.")
		}
		else{
			sendPush("TV is On.  TV was turned on within set time parameters and your quota of $maxTimeWk hours has not been reached.")
		}
	}
}

def sendNotifBad(){
		if(phone){
			sendSms(phone, "TV cannot be turned on.  Currently, either the time is either innapropriate or the quota for today has been reached.")
		}
		else{
			sendPush("TV cannot be turned on.  Currently, either the time is either innapropriate or the quota for today has been reached.")
		}
	
}

def checkOkay(){
	log.debug "made it to CheckOkay"
    def weekend = ["Sat","Sun"].contains(new Date().format("EEE"))
	if(weekend == true){
		if(state.totalTime < quotaWe){
			startTimer()
			sendNotifGood()
			
			
		}
		else if(state.totalTime >= quotaWe){
			sendNotifBad()
			tv.off()
		}
	}
	else if(weekend == false){
    	log.debug "made it to Weekday successfully"
         log.debug state.totalTime
		if(state.totalTime < quotaWk){
			log.debug "made it to quotaCheck successfully"
            startTimer()
			sendNotifGood()
			
		}
		else if(state.totalTime >= quotaWk){
			sendNotifBad()
			tv.off()
		}
	}
}

def tvHandler(evt) {
	log.debug "$evt.name: $evt.value"
	if (evt.numericValue > 0) {
    	tvHandlerOn()
    }
    else {
    	tvHandlerOff()
    }
}


def tvHandlerOn(){
	checkOkay()
	def freq = 1
	schedule("0 0/$freq * * * ?", checkOkay)	
}


def tvHandlerOff(){
	stopTimer()
    unschedule("checkOkay")
    def weekend = ["Sat","Sun"].contains(new Date().format("EEE"))
		if(weekend == true){
						
			state.remtime = (quotaWe - state.totalTime) / 3600000
			sendRemTime()
			
		}
		else if(weekend == false){
        log.debug "made it to remtime def"
		
		log.debug "total Time is $state.totalTime"
			state.remtime = (quotaWk - state.totalTime) / 3600000
			sendRemTime()
			
		}
}

def sendRemTime(){
	if(phone){
		sendSms(phone, "TV has been turned off.  You have $state.remtime hours remaining.")
	}
	else{
		sendPush("TV has been turned off.  You have $state.remtime hours remaining.")
	}
}
