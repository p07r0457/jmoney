
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
import org.jfree.data.time.TimeSeriesDataItem;

/**
 * @author Faucheux
 */
public class ActivLineChart extends LineChart {

    /**
     * 
     */
    private static final long serialVersionUID = -5937219526820401153L;

    /**
     * @param title
     * @param session
     */
    public ActivLineChart(String title, Session session, LineChartParameters params) {
        super(title, session);
        this.params = params;
    }

    
    /**
     * Calculate the values for each selected account.
     */
    protected void createOrUpdateValues(LineChartParameters param) {

        // Read the parameters.
        /*String*/Vector accountsToShow = param.accountList;
        int numberOfAccounts = accountsToShow.size();
        
        // Collect the dataes
        TimeSeries timeSeries[] = new TimeSeries[numberOfAccounts]; 
        for (int i=0; i<numberOfAccounts; i++) {
            String accountFullName = (String)accountsToShow.get(i);
            
            timeSeries[i] = getTimeSerieForAccount(accountFullName, session);
        }

        // Trick for BALANCE-Graph: we add all the dataes in the same Serie
        if (param.type == LineChartParameters.BALANCE) {
            TimeSeries balanceTimeSeries = new TimeSeries("BALANCE");
            for (int i=0; i<numberOfAccounts; i++) {
                mixTimeSeries(balanceTimeSeries, timeSeries[i], true /* the debit accounts are already nagative */);
            }
            timeSeries[0] = balanceTimeSeries;
            numberOfAccounts = 1;
        }
        
        // Create the collection of wanted curves
        if (data == null)
            data = new TimeSeriesCollection();
        else
            data.removeAllSeries();
        
        for (int i=0; i<numberOfAccounts; i++) {

            // Daily values and moving averages
            TimeSeries mav;
            
            if (params.daily)
                data.addSeries(timeSeries[i]);

            if (param.average30) {
                mav = MovingAverage.createMovingAverage(timeSeries[i], timeSeries[i].getName() + " (30 days)", 30, 30);
                data.addSeries(mav);
            }

            if (param.average120) {
                mav = MovingAverage.createMovingAverage(timeSeries[i], timeSeries[i].getName() + " (120 days)", 120, 120);
                data.addSeries(mav);
            }

            if (param.average365) {
                mav = MovingAverage.createMovingAverage(timeSeries[i], timeSeries[i].getName() + " (365 days)", 365, 365);
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
            } else  if (params.type == LineChartParameters.SALDO_ABSOLUT || params.type == LineChartParameters.BALANCE) {
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
            
            

            if (ChartsPlugin.DEBUG) System.out.println("Add to the graph: " + saldo + " at " + date);
            bts.add(date, new Double(saldo));
            date = (Day) date.next();
        }

        return bts;

    }

    /**
     * Mix two series of dataes by adding or substracting the
     * values of ts2 to the one of ts1.
     * @param ts1 			First series. It will be updated 
     * @param ts2			Second series. Won't be altered.
     * @param ts2IsToAdd	True if the values of ts2 have to be added to the one of ts1. 
     * 						False if they have to be substracted
     * @see the function addAndOrUpdate from TimeSeries. This function has perhaps the same role
     *      but isn't documented.  
     * @author Faucheux
     */
    void mixTimeSeries (TimeSeries ts1, TimeSeries ts2, boolean ts2IsToAdd) {
        for (int i=0; i<ts2.getItemCount(); i++) {
            TimeSeriesDataItem itemOfTs2 = (TimeSeriesDataItem) ts2.getDataItem(i).clone();
            TimeSeriesDataItem itemOfTs1 = ts1.getDataItem(itemOfTs2.getPeriod());
            if (! ts2IsToAdd) {
                itemOfTs2.setValue(new Long ( (-1) * itemOfTs2.getValue().longValue() ));
            }
            if (itemOfTs1 != null) {
                System.out.println("I'm adding " + itemOfTs1.getValue() + " and " + itemOfTs2.getValue());
                itemOfTs2.setValue(new Long(itemOfTs2.getValue().longValue() + itemOfTs1.getValue().longValue()));
            }
            System.out.println("I'm adding " + itemOfTs2.getValue() + " to ts1 for " + itemOfTs2.getPeriod());
            ts1.addOrUpdate(itemOfTs2.getPeriod(), itemOfTs2.getValue());
        }
    }
}
