
package net.sf.jmoney.charts;

import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.jfree.data.time.RegularTimePeriod;

/**
 * @author Faucheux
 * This class is only a container to collect all the parameters of
 * a LineChart.
 */
public class StackedChartParameters {

    public /*String*/Vector accountList;
    public Date	fromDate, toDate;
    public int maxLevel = 1;
    
    // Frequences of the dataes
    public static final int DAY = 0;
    public static final int MONTH = 1;
    public static final int YEAR  = 2;
    public int frequence = YEAR;
    
    // Contructor for the Object which represents the choosen TimePeriod: Month, Year...
    public Constructor regularPeriodConstructor = null;
    
    // Dateformat object to format the dates which appears in the graph.
    DateFormat dateformat = null;
    
    /**
     * 
     */
    public StackedChartParameters () {
        accountList = new Vector();
        fromDate = new Date(0); // 01.01.1970
        toDate = new Date(); // today
    }
    
    
    /**
     * @param accountList The accountList to set.
     */
    public void setAccountList(Vector accountList) {
        this.accountList = accountList;
    }
    /**
     * @param fromDate The fromDate to set.
     */
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }
    /**
     * @param toDate The toDate to set.
     */
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }
    
    public void setDates(String fromDate, String toDate) {
        DateFormat df = new SimpleDateFormat();
        try {
            setFromDate(df.parse(fromDate));
            setToDate(df.parse(toDate));
        } catch (ParseException e) {
            e.printStackTrace();
        };    
    }

    

    /**
     * @param frequence The frequence to set.
     */
    public void setFrequence(int frequence) {
        this.frequence = frequence;
    }
    
    /**
     * Instanciate the regularPeriodConstructor with the value dependant of periodClass
     * The dateformat is intanciated too
     * @author Faucheux
     */
    public void instanciateConstructor () {
        try {
	        Class periodClass = null;
	        String stringFormat = null;
	        switch (frequence) {
	        	case MONTH:
	        	    stringFormat = "MM.yy";
	        	    periodClass = Class.forName("org.jfree.data.time.Month");
	        	    break;
	        	case YEAR:
	        	    stringFormat = "yyyy";
	        	    periodClass = Class.forName("org.jfree.data.time.Year");
	        	    break;
	        	default: assert(false);
	        }
	        dateformat = new SimpleDateFormat(stringFormat);
	        regularPeriodConstructor = periodClass.getConstructor(new Class [] { Class.forName("java.util.Date") });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Create and returns a new object (Month, Day, Year...) which type depends of the choose Period
     * @param date
     * @return
     */
    public RegularTimePeriod createPeriod (Date date) {
        RegularTimePeriod re = null;
        try {
            re = (RegularTimePeriod) regularPeriodConstructor.newInstance(new Object [] {date});
        } catch (Exception e){
            e.printStackTrace();
        }
        return re;
    }
    /**
     * @param maxLevel The maxLevel to set.
     */
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
}
