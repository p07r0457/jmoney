
package net.sf.jmoney.charts;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

import org.jfree.data.MovingAverage;
import org.jfree.data.XYDataset;
import org.jfree.data.XYSeries;
import org.jfree.data.XYSeriesCollection;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * @author Faucheux
 */
public class ActivLineChart extends LineChart {

    /**
     * @param title
     * @param session
     */
    public ActivLineChart(String title, Session session) {
        super(title, session);
    }

    
    /**
     * Calculate the values for the all account.
     */
    protected void createOrUpdateValues(/*String*/Vector accountsToShow) {

        if (data == null)
            data = new TimeSeriesCollection();
        else
            data.removeAllSeries();
        
        
        for (int i=0; i<accountsToShow.size(); i++) {
            TimeSeries timeSeries = getTimeSerieForAccount((String)accountsToShow.get(i), session);
            data.addSeries(timeSeries);

            // Moving averages
            TimeSeries mav;
            
            mav = MovingAverage.createMovingAverage(
                    timeSeries, "30 day moving average", 30, 30);
            // data.addSeries(mav);
        }
        
        
    }
    
    
    /**
     * Calculate the values for an account.
     * @param acccount
     * @param session
     * @return
     */
    private TimeSeries getTimeSerieForAccount (String acccount, Session session) {
        
        TimeSeries bts = new TimeSeries(acccount);
        
        CapitalAccount a = (CapitalAccount) util.getAccountByFullName(session, acccount);
        Entry e = null;
        
        // Sort the entries chronologicaly
        List sortedEntries = util.getEntriesFromAccount(session, a);
        sortedEntries = util.sortChronogicalyEntries(sortedEntries);
        
        // If the first movement is after fromDate, set fromDate to this one
        e = (Entry)sortedEntries.get(0) ;
        if ( e.getTransaction().getDate().after(fromDate) )
            fromDate = e.getTransaction().getDate();
        
        Hashtable saldos = new Hashtable();
        Iterator it = sortedEntries.iterator();
        long saldo = 0; 
        while (it.hasNext()) {
            e = (Entry) it.next();
            saldo = saldo + e.getAmount();
            Day date = new Day (e.getTransaction().getDate());
            saldos.put(date, new Long(saldo));
        }
        
        // Now, enter them in the table
        Day date = new Day (fromDate);
        saldo = 0;
        while ( ! date.getEnd().after(toDate)) {
            if (saldos.containsKey(date)) {
                saldo = ((Long) saldos.get(date)).longValue()  / 100;
            }

            System.out.println("Add to the graph: " + saldo + " at " + date);
            bts.add(date, new Double(saldo));
            date = (Day) date.next();
        }

        return bts;

    }

}
