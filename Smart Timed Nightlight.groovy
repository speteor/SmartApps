/**
 *  Smart Nightlight MEGA
 *
 *  Author: Zach Naimon & Danny Kleinman
 *
 */
preferences {
	section("Select your light fictures for this App Instance"){
		input "lights", "capability.switch", multiple: true
	}
	section("Pick various methods of controlling the lights."){
		input "lightSensor", "capability.illuminanceMeasurement", title: "Light Sensor", required: false
		input "motionSensor", "capability.motionSensor", title: "Motion Sensor", required: false
		input "startTime", "time", title: "Start of A Time Window", required: false
		input "endTime", "time", title: "End of A Time Window", required: false
		input "zipCode", "text", title: "Zipcode"
		input "sunriseOffsetValue", "text", title: "Sunrise Offset (HH:MM)", required: false
		input "sunriseOffsetDir", "enum", title: "Sunrise Offset (Before or After)", required: false, metadata: [values: ["Before","After"]]
		input "sunsetOffsetValue", "text", title: "Sunset Offset (HH:MM)", required: false
		input "sunsetOffsetDir", "enum", title: "Sunset Offset (Before or After)", required: false, metadata: [values: ["Before","After"]]
	}
	section("And then off when there's no movement for x minutes.  (Defaults to 0 if left blank)"){
		input "delayMinutes", "number", title: "Theshold (Mins)", required: false
	} 
}



def installed() {
	initialize1()
	
}

def updated() {
	unsubscribe()
	initialize1()
}

def initalize(){
	if(!lightSensor && !motionSensor && !startTime && !endTime && !timeToDark & !timeToLight){
		sendPush("Please pick at least one method for the lights")
	}
	if(startTime && !endTime || endTime && !startTime){
		sendPush("Please fill in both values for the Time Window")
	}
	else{
		initialize2()
	}
}



def initialize2(){
	if(lightSensor){
	subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
	}
	if(motionSensor){
	subscribe(motionSensor, "motion.active", motionActiveHandler)
	subscribe(motionSensor, "motion.inactive", motionInactiveHandler)	
	}
	
	 
}

def illuminanceHandler(evt){
	if(timeWindowValCheck() == true){
		if(inTimeWindow() == true){
			if(!motionSensor){
				if(lightValueRange() == dark){
					lights.on()
					log.debug "It became dark and there is no motion sensor so we turned the lights on."	
				}
				else if(lightValueRange() == light){
					lights.off()
					log.debug "It became light and there is no motion sensor so we turned the lights off."
				}
			}
			else if(motionSensor){
				if(lightValueRange() == dark){
					lights.on()
					log.debug "It became dark so we turned the lights on."	
				}
				else if(lightValueRange() == light){
					log.debug "It became light, but because there's a motion sensor, we shouldn't turn the lights off."
				}	
			}
		}
		else if(inTimeWindow() == false){
		lights.off()
		log.debug "Not in Time Window That was provided.  Turning lights off."	
		}
	}
	else if(timeWindowValCheck == false && timeWindowOrSunRiseSet() == sunRiseSet){
		if(astroCheck() == sunrise || astroCheck() == sunset){
		if(!motionSensor){
			if(lightValueRange() == dark){
				lights.on()
				log.debug "It became dark and there is no motion sensor so we turned the lights on."	
			}
			else if(lightValueRange() == light){
				lights.off()
				log.debug "It became light and there is no motion sensor so we turned the lights off."
			}
		}
		else if(motionSensor){
			if(lightValueRange() == dark){
				lights.on()
				log.debug "It became dark so we turned the lights on."	
			}
			else if(lightValueRange() == light){
				log.debug "It became light, but because there's a motion sensor, we shouldn't turn the lights off."
			}	
		}
	}
	else{
		if(!motionSensor){
			if(lightValueRange() == dark){
				lights.on()
				log.debug "It became dark and there is no motion sensor so we turned the lights on."	
			}
			else if(lightValueRange() == light){
				lights.off()
				log.debug "It became light and there is no motion sensor so we turned the lights off."
			}
		}
		else if(motionSensor){
			if(lightValueRange() == dark){
				lights.on()
				log.debug "It became dark so we turned the lights on."	
			}
			else if(lightValueRange() == light){
				log.debug "It became light, but because there's a motion sensor, we shouldn't turn the lights off."
			}	
		}	
	}
}
}
def motionActiveHandler(evt){
	if(timeWindowValCheck() == true){
		if(inTimeWindow() == true){
		unschedule("checkTime")
  	   	log.debug "motion detected."
		if(lightValueRange() == dark){
   			log.debug "Motion was detected and light sensor read less than 30 lux.  Lights will be turned on."
  			lights.on()
        	state.stopTime = null
     	}
   	 	else if(lightValueRange == light){
    		log.debug "Enough lights are already on... no need to change anything."
        
    	}
		}
		else if(inTimeWindow() == false){
			lights.off()
			log.debug "Not in Time Window.  Lights will be turned off."
		}
	}
	else if(timeWindowValCheck() == false){
		unschedule("checkTime")
  	   	log.debug "motion detected."
		if(lightValueRange() == dark){
   			log.debug "Motion was detected and light sensor read less than 30 lux.  Lights will be turned on."
  			lights.on()
        	state.stopTime = null
     	}
   	 	else if(lightValueRange == light){
    		log.debug "Enough lights are already on... no need to change anything."
        
    	}
	}
}

def motionInactiveHandler(evt){
	log.debug "lack of motion detected."
	if(lightValueRange() == light){
    	log.debug "Motion has stopped, lights were on.  Starting Timer..."
        state.stopTime = now()
        def freq = 1
        schedule("0 0/$freq * * * ?", checkTime)       
    }
    else if(lightValueRange() == dark){
    	"Lights are already off and movement has not been detected.  No need to change anything."
    }
}

def checkTime(){
	def difTime = now() - state.stopTime
    def delayMS = delayMinutes * 60000
    if(difTime - delayMS >= 0){
    	log.debug "Lights are on, inactive motion buffer zone has expired.  Lights will now turn off."
        lights.off()
        unschedule("checkTime")    
    }
    else{
    	log.debug "something failed here..."
    }
}

private astroCheck() {
	def now = new Date()
	def geo = getWeatherFeature("geolookup", zipCode)?.location
	def astro = getWeatherFeature("astronomy", zipCode)?.moon_phase

	def tz = TimeZone.getTimeZone(geo.tz_long)
	def ldf = new java.text.SimpleDateFormat("yyyy-MM-dd")
	ldf.setTimeZone(tz)
	def today = ldf.format(now)

	def ltf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
	ltf.setTimeZone(tz)

	def sunrise = ltf.parse("${today} ${astro.sunrise.hour}:${astro.sunrise.minute}")
	def sunset = ltf.parse("${today} ${astro.sunset.hour}:${astro.sunset.minute}")

	log.debug "sunrise: $sunrise"
	log.debug "sunset: $sunset"
	riseTime = sunriseOffsetValue ? new Date(sunrise.time + sunriseOffset) : sunrise
	setTime = sunsetOffsetValue ? new Date(sunset.time + sunsetOffset) : sunset
	log.debug "riseTime: $riseTime"
	log.debug "setTime: $setTime"
	def result
	if(riseTime.before(now)){
		return sunrise
	}
	if(setTime.after(now)){
		return sunset
	}
	result
}


private inTimeWindow(){
	def now = now()
	def tz = timeZone
	def result
	if(timeWindowOrSunRiseSet() == timeWindow){
		if(now > timeToday(startTime, tz).time && now < timeToday(endTime, tz).time){
			return true
		}
		else{
			return false
		}
		result
	}
	else if(timeWindowOrSunRiseSet() == sunRiseSet){
		return false
	}
}

private lightValueRange(){
	def result
	if(lightSensor.currentIlluminance >= 30){
		return light
	}
	else if(lightSensor.currentIlluminance < 30){
		return dark
	}
	result
	
}	
private timeToLightDarkValCheck(){
	def result
	if(!sunriseOffsetValue && !sunsetOffsetValue){
		return false
	}
	else if(sunriseOffsetValue && sunsetOffsetValue || sunriseOffsetValue && !sunsetOffsetValue || sunsetOffsetValue && !sunriseOffsetValue){
		return true
	}
	result
}


private timeWindowValCheck(){
	def result
	if(!endTime && !startTime){
		return false
	}
	else if(starTime && endTime){
		return true
	}
	result
}

private timeWindowOrSunRiseSet(){
	def result
	if(timeToLightDarkValCheck() == true && timeWindowValCheck() == false){
	return sunRiseSet	
	}
	else{
	return timeWindow	
	}
	result
}
private getSunriseOffset() {
	offset(sunriseOffsetValue, sunriseOffsetDir)
}

private getSunsetOffset() {
	offset(sunsetOffsetValue, sunsetOffsetDir)
}

private offset(value, direction) {
	def result
	if (value.contains(":")) {
		def segs = value.split(":")
		result = (segs[0].toLong() * 3600000L) + (segs[1].toLong() * 60000L)
	}
	else {
		result = Math.round(value.toDouble() * 60000)
	}
	if (direction?.equalsIgnoreCase("before")) {
		result = -result
	}
	log.debug "value: '$value', direction: '$direction', result: $result"
	result
}
