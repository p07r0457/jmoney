
package net.sf.jmoney.charts;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IEntryQueries;
import net.sf.jmoney.model2.Session;

import org.jfree.data.time.RegularTimePeriod;

/**
 * Toolbox-Class for the access to the dataes.
 * This class won't be instanced.
 * 
 * TODO - Faucheux This class should be defined as a plug-in
 * @author Faucheux
 */
public class Util {

    /**
     * returns as Vector the list (not ordered) the accounts till the wished level (0 are the 'root' accounts)
	 * @author Faucheux
	 */
    public static  Vector getAccountsUntilLevel(Session session, int level) {
    	Vector v = new Vector();
    	Iterator it = session.getAccountIterator();
    	while (it.hasNext()) {
    		Account a = (Account) it.next();
    		v.add(a);
    		addSubAccountsUntilLevelToVector(a,level,v);
    	}
    	
    	return v;
    }
    
    private static void addSubAccountsUntilLevelToVector(Account a, int level, Vector v) {
        if (level == 0) return;
        Iterator it = a.getSubAccountIterator();
        while (it.hasNext()) {
            Account sa = (Account) it.next();
            v.add(sa);
            addSubAccountsUntilLevelToVector(sa,level-1,v);
        }
    }

    
    
    /**
     *  returns a new date, which is the last minute of the month of the given date
     * @author Faucheux
     * TODO: Do not use depreceated functions.
     */
    public static Date endOfMonth(Date givenDate) {
        GregorianCalendar gc = new GregorianCalendar(givenDate.getYear(), givenDate.getMonth(), givenDate.getDay());
        Date endOfMonth = new Date();
        endOfMonth.setYear(givenDate.getYear());
        endOfMonth.setMonth(givenDate.getMonth());
        endOfMonth.setDate(gc.getMaximum(GregorianCalendar.MONTH));
        endOfMonth.setHours(23);
        endOfMonth.setMinutes(59);
        
        return endOfMonth;
    }
 
    /**
     * @author Faucheux
     */
	public static Account getAccountByFullName(Session session, String name) throws Error {

	    Account a = null;
	    String begin = null;
	    String rest = null;
	    Account result = null;
	    if (name.indexOf('.') != -1) {
	        rest = name.substring(name.indexOf('.')+1);
	        begin = name.substring(0,name.indexOf('.'));
	    } else {
	        begin=name;
	    }
	    Iterator it = session.getAccountIterator();
	    while (it.hasNext()) {
	        a = (Account) it.next();
	        if (ChartsPlugin.DEBUG) System.out.println("Compare " + begin + " to " + a.getName());
	        if (a.getName().equals(begin)) {
	            result = getSubAccountByFullName(a,rest);
	            break;
	        }
	    }
	    if (result == null) throw new Error ("The account >" + name + "< can't be found");
	    return result;
	}

	private static Account getSubAccountByFullName(Account a, String name) {
	    Account sa = null;
	    String begin = null;
	    String rest = null;
	    Account result = null;
	    
	    if (name == null) {
	        result = a;
	    } else {
		    if (name.indexOf('.') != -1) {
		        begin = name.substring(0,name.indexOf('.'));
		        rest = name.substring(name.indexOf('.')+1);
		    } else {
		        begin=name;
		    }
		    Iterator it = a.getSubAccountIterator();
		    while (it.hasNext()) {
		        sa = (Account) it.next();
		        if (ChartsPlugin.DEBUG) System.out.println("Compare " + begin + " to " + sa.getName());
		        if (sa.getName().equals(begin))
	                result = getSubAccountByFullName(sa,rest);
		    }
	    }
	    
	    return result;
	}
	

	public static List getEntriesFromAccount (Session session,
            CapitalAccount a) {
	    
        List entries = new LinkedList();
        
        Iterator it = a.getEntries().iterator();
        while (it.hasNext()) {
            entries.add(it.next());
        }

        return entries;
	}
	

	public static List getEntriesFromAccountBetweenDates (Session session,
            CapitalAccount a, Date fromDate, Date toDate) {
	    
        List entries = new LinkedList();
        Iterator it = a.getEntries().iterator();
        while (it.hasNext()) {
            Entry e = (Entry) it.next();
            if (e.getTransaction().getDate().after(fromDate)
                    && e.getTransaction().getDate().before(toDate))
                entries.add(e);
        }

        return entries;
	}
	
	
	/**
	 * return the list of the entries of an account, chronlogicaly ordered.
	 * @param session
	 * @param a
	 * @return
	 */
	public static List sortChronogicalyEntries(List entries) {
	    

        Collections.sort(entries, new Comparator() {
            public int compare(Object a, Object b) {
                return ((Entry) a).getTransaction().getDate().compareTo(
                        ((Entry) b).getTransaction().getDate());
            }
        });

        return entries;
    }
	
	/**
	 * return a list of all entries of the given account and its subaccounts.
	 * @param session
	 * @param a
	 * @return
	 */
	public static List getEntriesFromAccountAndSubaccounts (Session session, CapitalAccount a) {
		if (ChartsPlugin.DEBUG) System.out.println("getEntriesFromAccountAndSubaccounts for " + a.getName());
	    List entries = getEntriesFromAccount(session, a);
	    Iterator it = a.getSubAccountIterator();
	    while (it.hasNext()) {
	        CapitalAccount subaccount = (CapitalAccount) it.next();
	        entries.addAll(getEntriesFromAccountAndSubaccounts(session, subaccount));
	    }
	    return entries;
	}
	
	/**
	 * return a list of all directs subaccounts.
	 * @param session
	 * @param a
	 * @return
	 */
	public static List getSubAccounts (CapitalAccount a) {
		if (ChartsPlugin.DEBUG) System.out.println("getSubAccounts for " + a.getName());
	    return getSubAccountsUntilLevel (a, a.getLevel() + 1);
	}

	/**
	 * return a list of all subaccounts until the given level.
	 * @param session
	 * @param a
	 * @return
	 */
	public static List getSubAccountsUntilLevel (CapitalAccount a, int level) {
		if (ChartsPlugin.DEBUG) System.out.println("getSubAccountsUntilLevel for " + a.getName() + ", Level " + level);
        List subaccounts = new LinkedList();
        if (a.getLevel() < level) {
            Iterator it = a.getSubAccountIterator();
            while (it.hasNext()) {
                CapitalAccount sa = (CapitalAccount) it.next(); 
                subaccounts.add(sa);
	            subaccounts.addAll(getSubAccountsUntilLevel(sa, level));
	        }
	    }
	    return subaccounts;
	}

	
	/**
	 * Return the sum of the mouvements between the two dates ; the list must be chronogically 
	 * sorted !
	 * 
	 * @param mvt
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public static long getSumMouvementsBetweenDates (List mvt, Date fromDate, Date toDate) {
	    long sum = 0;

	    Iterator it = mvt.iterator();
	    while (it.hasNext()) {
	        Entry e = (Entry) it.next();
	        if (e.getTransaction().getDate().after(toDate)) break;
	        if (! e.getTransaction().getDate().before(fromDate)) sum += e.getAmount();
	    }
	    return sum / 100;
	}

	/**
	 * @param date
	 * @param date2
	 * @param includeSubAccounts
	 * @return
	 */
	public static long [] getEntryTotalsByPeriod(CapitalAccount a, RegularTimePeriod startPeriod, int numberOfPeriods, boolean includeSubAccounts) {
		IEntryQueries queries = (IEntryQueries)a.getSession().getAdapter(IEntryQueries.class);
    	if (queries != null) {
    	    // TODO: correct it when the IEntryQueries are really implemented 
    		// return queries.getEntryTotalsByMonth(this, startYear, startMonth, numberOfMonths, includeSubAccounts);
    		long [] totals = new long[1];
    		return totals;
    	} else {
    		// IEntryQueries has not been implemented in the datastore.
    		// We must therefore provide our own implementation.
    		
    		Vector entriesList = new Vector();
    		entriesList.addAll(a.getEntries());
    		if (includeSubAccounts) {
    			a.addEntriesFromSubAccounts(a, entriesList);
    		}
    		
            Collections.sort(entriesList, new Comparator() {
                public int compare(Object a, Object b) {
                    assert (a instanceof Entry);
                    assert (b instanceof Entry);
                    return ((Entry) a).getTransaction().getDate().compareTo(
                            ((Entry) b).getTransaction().getDate());
                }
            });

    		
    		long [] totals = new long[numberOfPeriods];

    		// calculate the sum for each period
    		RegularTimePeriod period = startPeriod;
            for (int i=0; i<numberOfPeriods; i++) {
            	Date startOfPeriod = new Date(period.getFirstMillisecond());
            	Date endOfPeriod = new Date(period.getLastMillisecond());
            	period = period.next();
            	
            	int total = 0;
            	for (Iterator iter = entriesList.iterator(); iter.hasNext(); ) {
            		Entry entry = (Entry)iter.next();
            		if (ChartsPlugin.DEBUG) System.out.println(entry.getTransaction().getDate());
            		if (entry.getTransaction().getDate().compareTo(startOfPeriod) >= 0 
            		 && entry.getTransaction().getDate().compareTo(endOfPeriod) < 0) {
            			total += entry.getAmount();
            		}
            	}
            	totals[i] = total / 100;
            }
            
            return totals;
    	}
	}

}
