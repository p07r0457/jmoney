package net.sf.jmoney.fields;

import java.util.Calendar;
import java.util.Date;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.vafada.swtcalendar.SWTCalendar;
import org.vafada.swtcalendar.SWTCalendarEvent;
import org.vafada.swtcalendar.SWTCalendarListener;

public class DateControlAlwaysExpanded extends DateComposite {
	protected SWTCalendar swtcal;
	protected Text textControl;
	
    // TODO Listen to date format changes.
    private VerySimpleDateFormat fDateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());

	public DateControlAlwaysExpanded(Composite parent) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(1, false));
		
		swtcal = new SWTCalendar(this);
		textControl = new Text(this, SWT.NONE);
		
        swtcal.addSWTCalendarListener(
        		new SWTCalendarListener() {
        			public void dateChanged(SWTCalendarEvent calendarEvent) {
        				Date date = calendarEvent.getCalendar().getTime();
        				textControl.setText(fDateFormat.format(date));
        			}
        		});

	}

	public void setDate(Date date) {
		if (date == null) {
			textControl.setText("");
		} else {
			textControl.setText(fDateFormat.format(date));
			
   	        Calendar calendar = Calendar.getInstance();
	        calendar.setTime(date);
   	        swtcal.setCalendar(calendar);
		}
	}

	/**
	 * @return the date, or null if a valid date is not set in
	 * 				the control
	 */
	public Date getDate() {
        String text = textControl.getText();
        try {
        	return fDateFormat.parse(text);
        } catch (IllegalArgumentException e) {
        	return null;
        }
	}
}
