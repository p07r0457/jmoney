/*
 * Created on 6 sept. 2004
 */
package net.sf.jmoney.charts;

import java.awt.Frame;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.jmoney.charts.*;
import net.sf.jmoney.model2.*;

import com.jrefinery.data.DefaultPieDataset;

public class ExpensePieChart extends PieChart {

	protected Date fromDate; 
	protected Date toDate;
	
	public ExpensePieChart(String title, Session session) {
        super(title, session);
	}
	
	protected void setValues(Session session, DefaultPieDataset data) {

		// TODO: Faucheux - der gewählte Level sollte  parametrisierbar sein.
		int  maxLevel = 1;

		// collect the values
		TreeSet values = new TreeSet ();
		
	    Iterator aIt = session.getAccountsUntilLevel(maxLevel).iterator();
	    while (aIt.hasNext()) {
	        Account currentAccount = (Account) aIt.next();
	        long balance;
	        
	        if (currentAccount.getLevel() < maxLevel) {
	        	// If the account has sub accounts, they will have their own entry -> we don't have to include them here
	        	balance = currentAccount.getBalance(session, fromDate, toDate);
	        } else {
	        	// If the account has sub accounts, they won't have their own entry -> we have to include them here
	        	balance = currentAccount.getBalanceWithSubAccounts(session, fromDate, toDate);
	        }
	        
	        System.out.println(currentAccount.getName() + " : " + balance +".");
		    values.add(new CoupleStringNumber(currentAccount.getName(), balance));
	    }
	    
	    // set the (sorted) values in the graph
	    Iterator it = values.iterator();
	    while (it.hasNext()) {
	    	CoupleStringNumber csn = (CoupleStringNumber) it.next();
		    data.setValue(csn.s, new Long(csn.n));
	    }
	    
    }

	protected void initParameters() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
        	fromDate = df.parse("2004-01-01");
        } catch (ParseException e) {
        	System.err.println(e.getStackTrace());
        	Error err = new Error("ParseException");
        	throw err;
        }
    	toDate = new Date();

    	setTitle("Expenses from " + df.format(fromDate) + " to " + df.format(toDate));
	}

	/**
	 * Object to stock the String and Number the time to sort them in a TreeSet
	 * @author Faucheux
	 */
	private class CoupleStringNumber implements Comparable {
		public String s;
		public long n;
		
		
		public CoupleStringNumber(String s, long n) {
			this.s = s;
			this.n = n;
		}
		
		public int compareTo (Object csn2) {
		    // Compare first the values, than the names.
		    // If the names are the same too, then compare the hash codes: two objects 
		    // should be considered as equal iff they really are the same.
		    // Caution: to have the display well-displayed, we have to say "bigger first"
		    // and therefore inverse the results;
			if (csn2 instanceof CoupleStringNumber) {
			    String s2 = ((CoupleStringNumber)csn2).s;
			    long n2 = ((CoupleStringNumber)csn2).n;
			    int difference = (new Long(n)).compareTo(new Long(n2));
			    if (difference == 0) difference = s.compareTo(s2);
			    if (difference == 0) difference = s.hashCode() - s2.hashCode();
			    return - difference;
			} else {
				throw new Error("A CoupleStringNumber object can't be compared to an other typ of object.");
			}
			
		}

}
}
