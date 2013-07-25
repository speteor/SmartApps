/* httpPostJson(uri: 'http://siteurl.tld/dir', path: '',  body: [evt: [deviceId: evt.deviceId, name: evt.name, value: evt.value]]) {
	log.debug "Event data successfully posted"
} */
preferences {
	section("Check these switches") {
		input "switches", "capability.switch"
        input "days", "decimal", title: "For events in the past ... days"
		input "maxEvts", "number", title: "Maximum Number of Events sent. (All in interval if left blank)", required: false
        input "email", "email", title: "Email Address"
	}
}
def installed(){
	initialize()
}

def updated(){
	unsubscribe()
	initialize()
}

def initialize(){
	subscribe(app, appTouch)
}

def appTouch(evt){
	state.daysBefore = days as int
    log.debug state.daysBefore
	state.daysBeforeMS = state.daysBefore * 86400000
    def startTime = now() - state.daysBeforeMS
    def endTime = now()
    def startEvt = new Date(startTime)
    def endEvt = new Date(endTime)
   	log.debug "Days before is $state.daysBefore"
    log.debug "EventsBetween StartTime is $startEvt"
    log.debug "EventsBetween EndTime is $endEvt"
 	if(maxEvts){
    def evts = switches.eventsBetween(startEvt, endEvt, [max: maxEvts])
    log.debug evts
    def evtSize = evts.size()
    def label = switches.name
    def html = "<!DOCTYPE html><html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body style='margin: 0; padding: 5px; padding-top: 30px; padding-bottom: 30px; background: #25837d;'><div style='width: 85%; margin-left: auto; margin-right: auto; background: #fff; padding: 10px;'><img src='http://smartthings.com/img/smartthings-logo.png' style='width: 225px'><br />"
    html += "<h2 style='font-family: arial; font-weight: bold; color: #666; text-align: left; font-size: 18px;'> Last ${evtSize} Events for ${label} </h2>"
    evts.each{
		def now = now()
        def evtTime = it.date.getTime()
        def hoursBefore = Math.round(((now - evtTime) / 3600000) * 100) / 100
        if(hoursBefore < 24){
             if(hoursBefore == 1){
             html += "<div style='font-family: arial; color: #aaa;'><span><strong>${hoursBefore} Hour Ago</strong> - ${it.value} </div><br />"
             }
             else{
             html += "<div style='font-family: arial; color: #aaa;'><span><strong>${hoursBefore} Hours Ago</strong> - ${it.value} </div><br />"
             }
        }
        else if(hoursBefore >= 24){
        	def dayFix = (hoursBefore / 24) as int
            def hourFix = Math.round((hoursBefore - (24 * dayFix)) * 100) / 100
            if(dayFix == 1){
            	if(hourFix == 1){
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Day and ${hourFix} Hour Ago</strong> - ${it.value} </div><br />"
                }
                else{
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Day and ${hourFix} Hours Ago</strong> - ${it.value} </div><br />"
                }
            }
            else{
            	if(hourFix == 1){
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Days and ${hourFix} Hour Ago</strong> - ${it.value} </div><br />"	
                }
                else{
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Days and ${hourFix} Hours Ago</strong> - ${it.value} </div><br />"
                }
            
            }
        }
       
       
	}
    log.debug "First Event Date: ${evts[-1].date}"
    
    log.debug "Event String Size: ${evtSize}"
	def evtDifTime = Math.round(((evts[0].date.getTime() / 3600000) - (evts[-1].date.getTime() / 3600000)) * 100) / 100 
    def avgTime = evtDifTime / evts.size()
    log.debug "The DifTime is ${evtDifTime} and avgTime is ${avgTime}"
    html += "<div><h2 style='font-family: arial; font-weight: bold; color: #666; text-align: left; font-size: 18px;'>Event Summary for ${label}</h2></div>"
	html += "<div style='font-family: arial; color: #aaa;'><span>First Event: ${evts[-1].value}</span></div> "
	html += "<div style='font-family: arial; color: #aaa;'><span>Last Event: ${evts[0].value}</span></div>"
	html += "<div style='font-family: arial; color: #aaa;'><span>Time Between First and Last Events: ${evtDifTime} hours</span></div>"
    html += "<div style='font-family: arial; color: #aaa;'><span>Average Time Between Events: ${avgTime} hours</span></div>"
	html += "</div></div></body></html>"
   
    def emailSub = "Your SmartThings Summary for ${switches.label}"
    def emailAddr = email
    httpPostJson(uri: "http://megapixelsoftware.com/smartthings/e.php", body: [email: emailAddr, body: html, subject: "Hi"]) {responce ->
		log.debug responce.data
	}
    }
    else if(!maxEvts){
     def evts = switches.eventsBetween(startEvt, endEvt)
    log.debug evts
    def evtSize = evts.size()
    def label = switches.name
    def html = "<!DOCTYPE html><html><head><meta http-equiv='Content-Type' content='text/html; charset=UTF-8'></head><body style='margin: 0; padding: 5px; padding-top: 30px; padding-bottom: 30px; background: #25837d;'><div style='border-radius: 5px; -moz-border-radius: 5px; -webkit-border-radius: 5px; width: 85%; margin-left: auto; margin-right: auto; background: #fff; padding: 10px;'><img src='http://smartthings.com/img/smartthings-logo.png' style='width: 225px'><br />"
    html += "<h2 style='font-family: arial; font-weight: bold; color: #666; text-align: left; font-size: 18px;'> Last ${evtSize} Events for ${label} </h2>"
    evts.each{
		def now = now()
        def evtTime = it.date.getTime()
        def hoursBefore = Math.round(((now - evtTime) / 3600000) * 100) / 100
        if(hoursBefore < 24){
             if(hoursBefore == 1){
             html += "<div style='font-family: arial; color: #aaa;'><span><strong>${hoursBefore} Hour Ago</strong> - ${it.value} </div><br />"
             }
             else{
             html += "<div style='font-family: arial; color: #aaa;'><span><strong>${hoursBefore} Hours Ago</strong> - ${it.value} </div><br />"
             }
        }
        else if(hoursBefore >= 24){
        	def dayFix = (hoursBefore / 24) as int
            def hourFix = Math.round((hoursBefore - (24 * dayFix)) * 100) / 100
            if(dayFix == 1){
            	if(hourFix == 1){
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Day and ${hourFix} Hour Ago</strong> - ${it.value} </div><br />"
                }
                else{
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Day and ${hourFix} Hours Ago</strong> - ${it.value} </div><br />"
                }
            }
            else{
            	if(hourFix == 1){
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Days and ${hourFix} Hour Ago</strong> - ${it.value} </div><br />"	
                }
                else{
                html += "<div style='font-family: arial; color: #aaa;'><span><strong>${dayFix} Days and ${hourFix} Hours Ago</strong> - ${it.value} </div><br />"
                }
            
            }
        }
       
       
	}
    log.debug "First Event Date: ${evts[-1].date}"
    
    log.debug "Event String Size: ${evtSize}"
	def evtDifTime = Math.round(((evts[0].date.getTime() / 3600000) - (evts[-1].date.getTime() / 3600000)) * 100) / 100 
    def avgTime = evtDifTime / evts.size()
    log.debug "The DifTime is ${evtDifTime} and avgTime is ${avgTime}"
    html += "<div><h2 style='font-family: arial; font-weight: bold; color: #666; text-align: left; font-size: 18px;'>Event Summary for ${label}</h2></div>"
	html += "<div style='font-family: arial; color: #aaa;'><span><strong>First Event:</strong> ${evts[-1].value}</span></div> "
	html += "<div style='font-family: arial; color: #aaa;'><span><strong>Last Event:</strong> ${evts[0].value}</span></div>"
	html += "<div style='font-family: arial; color: #aaa;'><span><strong>Time Between First and Last Events:</strong> ${evtDifTime} hours</span></div>"
    html += "<div style='font-family: arial; color: #aaa;'><span><strong>Average Time Between Events:</strong> ${avgTime} hours</span></div>"
	html += "</div></div></body></html>"
   	
    def emailSub = "Your SmartThings Summary"

    
    def emailAddr = email
    httpPostJson(uri: "http://megapixelsoftware.com/smartthings/e.php", body: [email: emailAddr, body: html, subject: emailSub]) {responce ->
		log.debug responce.data
	}
    }
       

}
