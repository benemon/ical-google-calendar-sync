package com.redhat.bh.pt.gcal.helper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


/**
 * @version 
 */
@XmlRootElement(name = "PTCalendars")
@XmlAccessorType(XmlAccessType.FIELD)
public class CalendarInfoDocument {
	  
    @XmlElement(name = "calendar")
    private List<CalendarInfo> PTCalendars = null;
 
    public List<CalendarInfo> getPTCalendars() {
        return PTCalendars;
    }
 
    public void setPTCalendars(List<CalendarInfo> PTCalendars) {
        this.PTCalendars = PTCalendars;
    }
    
}
