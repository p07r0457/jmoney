
package net.sf.jmoney.charts;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 * @author Faucheux
 * This class is only a container to collect all the parameters of
 * a LineChart.
 */
public class LineChartParameters {

    // Differents typs
    public static final int SALDO_ABSOLUT = 1;
    public static final int SALDO_RELATIV = 2;
    public static final int MOUVEMENT = 3;
    public static final int BALANCE = 4;
    
    public /*String*/Vector accountList;
    public Date	fromDate, toDate;
    
    // Frequences of the dates
    public boolean	daily;
    public boolean	average30;
    public boolean	average120;
    public boolean	average365;
    
    // Type of the graph (for the time, SALDO or MOUVEMENT)
    public int type = SALDO_ABSOLUT;
    
    // Do we include the subaccounts for each account?
    public boolean withSubaccounts;
    
    /**
     * 
     */
    public LineChartParameters () {
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
    /**
     * @param average120 The average120 to set.
     */
    public void setAverage120(boolean average120) {
        this.average120 = average120;
    }
    /**
     * @param average30 The average30 to set.
     */
    public void setAverage30(boolean average30) {
        this.average30 = average30;
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
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }
    /**
     * @param daily The daily to set.
     */
    public void setDaily(boolean daily) {
        this.daily = daily;
    }
    /**
     * @param withSubaccounts The withSubaccounts to set.
     */
    public void setWithSubaccounts(boolean withSubaccounts) {
        this.withSubaccounts = withSubaccounts;
    }
    /**
     * @param average365 The average365 to set.
     */
    public void setAverage365(boolean average365) {
        this.average365 = average365;
    }
}
