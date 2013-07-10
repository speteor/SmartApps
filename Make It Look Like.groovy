/**
 *  Make It Look Like
 *
 *  Author: SmartThings
 */

preferences {
	section("When I touch the app, turn on...") {
		input "switches", "capability.switch"
        
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
 	def evts = switches.eventsBetween(startEvt, endEvt, [max: 1])
    log.debug "We have $evts.length events in that time window"
    evts.each{
        if(it.name == "switch"){
        log.debug it.value 
        log.debug it.date
        	if(it.value == "on"){
            	switches.on()
            }
            else if(it.value == "off"){
            	switches.off()
            }
       }
    }
	
}
