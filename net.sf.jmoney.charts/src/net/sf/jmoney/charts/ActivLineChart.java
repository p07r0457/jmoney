
package net.sf.jmoney.charts;

import java.util.Date;
import java.util.Iterator;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccountImpl;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

import com.jrefinery.data.BasicTimeSeries;
import com.jrefinery.data.Day;
import com.jrefinery.data.TimePeriod;
import com.jrefinery.data.XYSeries;

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
     *
     */

    protected XYSeries createValues(Session session) {
        XYSeries bts = new XYSeries("Hello!");
        long saldo = 0;
        
        CapitalAccountImpl a = (CapitalAccountImpl) session.getAccountByFullName("Banques.Postbank");
        Entry e = null;
        Iterator it = a.entries.iterator() ;
        Date currentDate = null;
        while (it.hasNext()) {
            e = (Entry) it.next();
            if (e.getTransaxion().getDate().equals(currentDate)) {
               saldo += e.getAmount();
           } else {
               System.out.println("Add to the graph: " + saldo + " at " + e.getTransaxion().getDate().getTime());
               bts.add(e.getTransaxion().getDate().getTime(), saldo / 100);
               saldo += e.getAmount();
               currentDate = e.getTransaxion().getDate();
           }
        }
        // TODO Faucheux - Shouldn't we add the last entry?
        // if ( e != null ) bts.add(e.getTransaxion().getDate().getTime(), saldo);

        return bts;
    }

}
