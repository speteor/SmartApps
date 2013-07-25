/**
 *  Make It Look Like
 *
 *  Author: SmartThings
 */

preferences {
	section("Utilize these swithces") {
		input "switches", "capability.switch", multiple: true
        
	}
}

def installed()
{
	initialize()
    
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize(){
	def freq = 10
	schedule("0 0/$freq * * * ?", runJob)
    def day = new Date(now())
    def midnight = timeTodayAfter(day, "00:00", location.timeZone)
    runDaily(midnight, newDay)
    
}

def newDay(){
	state.daysBefore = (new Random().nextInt(4)) + 1
    state.daysBeforeMS = state.daysBefore * 86400000
}
	
	
def runJob() {	
    def startTime = now() - state.daysBeforeMS
    def endTime = startTime + 600000
    def startEvt = new Date(startTime)
    def endEvt = new Date(endTime)
   	log.debug "Days before is $state.daysBefore"
    log.debug "EventsBetween StartTime is $startEvt"
    log.debug "EventsBetween EndTime is $endEvt"	
    log.debug "We have $evts.length events in that time window"
    if(switches.size() == 1){
		def evts = switches.eventsBetween(startEvt, endEvt, [max: 1])
		evts.each{
			if(it.name == "switch"){
				if(it.value == "on"){
					switches.on()
				}
				else if(it.value == "off"){
					switches.off()
				}
			}
		}
	}
	else{		
		switches.each{ 
			for(i in 0..1){
				def evts = it.eventsBetween(startEvt, endEvt, [max: 1])
				if(evts[i].name == "switch"){
        			log.debug evts[i].value 
        			log.debug evts[i].date
        			if(evts[i].value == "on"){
          				it.on()
         			}
         			else if(evts[i].value == "off"){
        				it.off()
       		 		}
				}
      	  	}
    	}
	}
	
}
