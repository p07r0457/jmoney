
package net.sf.jmoney.charts;

import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

import org.jfree.data.MovingAverage;
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
    public ActivLineChart(String title, Session session, LineChartParameters params) {
        super(title, session);
        this.params = params;
    }

    
    /**
     * Calculate the values for the all account.
     */
    protected void createOrUpdateValues(LineChartParameters param) {

        // Read the parameters.
        /*String*/Vector accountsToShow = param.accountList;
        
        if (data == null)
            data = new TimeSeriesCollection();
        else
            data.removeAllSeries();
        
        
        for (int i=0; i<accountsToShow.size(); i++) {
            String accountFullName = (String)accountsToShow.get(i);
            
            TimeSeries timeSeries = getTimeSerieForAccount(accountFullName, session);
            if (params.daily)
                data.addSeries(timeSeries);

            // Moving averages
            TimeSeries mav;
            
            if (param.average30) {
                mav = MovingAverage.createMovingAverage(timeSeries, accountFullName + " (30 days)", 30, 30);
                data.addSeries(mav);
            }

            if (param.average120) {
                mav = MovingAverage.createMovingAverage(timeSeries, accountFullName + " (120 days)", 120, 120);
                data.addSeries(mav);
            }

            if (param.average365) {
                mav = MovingAverage.createMovingAverage(timeSeries, accountFullName + " (365 days)", 365, 365);
                data.addSeries(mav);
            }

        }
    }
    
    
    /**
     * Calculate the values for an account.
     * @param acccount
     * @param session
     * @return
     */
    private TimeSeries getTimeSerieForAccount (String acccount, Session session) {
        Date fromDateForThisAccount = params.fromDate;
        
        TimeSeries bts = new TimeSeries(acccount);
        if (params.withSubaccounts) bts.setName(bts.getName() + " (and sub.)");
        
        CapitalAccount a = (CapitalAccount) Util.getAccountByFullName(session, acccount);
        Entry e = null;
        
        // Sort the entries chronologicaly
        List sortedEntries; 
        if (params.withSubaccounts) 
            sortedEntries = Util.getEntriesFromAccountAndSubaccounts(session, a);
        else
            sortedEntries = Util.getEntriesFromAccount(session, a);
        
        sortedEntries = Util.sortChronogicalyEntries(sortedEntries);
        
        // If the first movement is after fromDate, set fromDate to this one
        e = (Entry)sortedEntries.get(0) ;
        if ( e.getTransaction().getDate().after(params.fromDate) )
            fromDateForThisAccount = e.getTransaction().getDate();
        
        Hashtable saldos = new Hashtable();
        Iterator it = sortedEntries.iterator();
        long saldo = 0;
        Day dateOfPreviousMouvement = null; 
        while (it.hasNext()) {
            e = (Entry) it.next();
            Day date = new Day (e.getTransaction().getDate());

            if (params.type == LineChartParameters.MOUVEMENT) {
                // When calculating the MOUVEMENT, we have to sum the entries on a day only.
                if (date.compareTo(dateOfPreviousMouvement) != 0) saldo = 0;
            	saldo = saldo + e.getAmount();
            } else  if (params.type == LineChartParameters.SALDO_ABSOLUT) {
                // Saldo absolut: the entry is added to the last result
            	saldo = saldo + e.getAmount();
            } else if (params.type == LineChartParameters.SALDO_RELATIV) {
                // Saldo relativ: the entry is added only if after the begin of the
                // chart
                if (e.getTransaction().getDate().after(fromDateForThisAccount)) 
                	saldo = saldo + e.getAmount();
            }

            saldos.put(date, new Long(saldo));
            dateOfPreviousMouvement = date;
        }
        
        // Now, enter them in the table
        Day date = new Day (fromDateForThisAccount);
        saldo = 0;
        while ( ! date.getEnd().after(params.toDate)) {
            if (saldos.containsKey(date)) {
                saldo = ((Long) saldos.get(date)).longValue()  / 100;
            } else if (params.type == LineChartParameters.MOUVEMENT){
                saldo = 0;
            }
            
            

            System.out.println("Add to the graph: " + saldo + " at " + date);
            bts.add(date, new Double(saldo));
            date = (Day) date.next();
        }

        return bts;

    }

}
