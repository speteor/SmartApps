/**
 *  Turn It On When I'm Here
 *
 *  Author: SmartThings
 */
preferences {
	section("When I arrive and leave..."){
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Turn on/off a light..."){
		input "lights", "capability.switch", multiple: true
	}
}

def installed()
{
	subscribe(presence1, "presence", presenceHandler)
}

def updated()
{
	unsubscribe()
	subscribe(presence1, "presence", presenceHandler)
}

def presenceHandler(evt)
{
	def current = presence1.currentValue("presence")
    log.debug current
    def presenceValue = presence1.find{it.currentPresence == "present"}
   	log.debug presenceValue
    if(presenceValue){
    	lights.on()
        log.debug "Someone's home!"
    }
    else{
    	lights.off()
        log.debug "Everyone's away."
    }
}
