package net.sf.jmoney.charts;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

import org.jfree.data.CategoryDataset;
import org.jfree.data.DefaultCategoryDataset;
import org.jfree.data.statistics.Statistics;
import org.jfree.data.time.Month;

import sun.util.calendar.CalendarDate;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;

public class StackedAccountChart extends StackedChart {

    private String accounts[];
    
    public StackedAccountChart(String accounts[], Session session) {
        super(accounts, session);
    }

    protected void initParameters (String accounts[], Session session) {
        this.accounts = accounts;
    };

    protected CategoryDataset createValues(Session session) {
        long saldo = 0;
        Date[] dates;
        int numberOfAccounts = 0;
        int numberOfMonths = 10; // TODO: Faucheux - To change (count)
        Date fromDate = null;
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        List listAccounts;
        long totalValue = 0;
        int maxLevel = 2; // TODO: Faucheux - as parameter
        
        try {
        	fromDate = df.parse("01.01.2004");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        Date toDate = new Date(); // today

        // Calculate the begin and the end of each Time-Gap
        Date [] dateBegin = new Date[numberOfMonths];
        Date [] dateEnd = new Date[numberOfMonths];
        String[] dateLabels = new String[numberOfMonths];
        dateBegin[0] = fromDate;
        for (int i=1; i<numberOfMonths; i++) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(dateBegin[i-1]);
            gc.add(Calendar.MONTH, 1);
            dateBegin[i] = gc.getTime();
            gc.add(Calendar.DAY_OF_MONTH, -1);
            dateEnd[i-1] = gc.getTime();
        }
        dateEnd[numberOfMonths-1] = toDate;
        for (int i=0; i<numberOfMonths; i++) 
            dateLabels[i] = (new SimpleDateFormat("MM.yy")).format(dateBegin[i]);
        
        
        // Get the accounts
        listAccounts = new LinkedList();
        for (int i=0; i<accounts.length; i++) {
            CapitalAccount acc = (CapitalAccount) util.getAccountByFullName(session, accounts[i]);
            listAccounts.add(acc);
            listAccounts.addAll(
                    util.getSubAccountsUntilLevel(acc, maxLevel)
                    );
        }
        
        numberOfAccounts = listAccounts.size();
        
        // Now, enter the values in our table
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List accounts = new LinkedList(); 
        
        // for each account
        int currentAccount = 0;
        Iterator itAccounts = listAccounts.iterator();
        while (itAccounts.hasNext()) { 
            CapitalAccount a = (CapitalAccount) itAccounts.next();
            accountCategory accountCategory = new accountCategory(a.getName(), 0);
            // get the entries
            List entriesList; 
	        if (a.getLevel() < maxLevel) {
	        	// If the account has sub accounts, they will have their own entry -> we don't have to include them here
	            entriesList = util.getEntriesFromAccount(session, a);
	        } else {
	        	// If the account has sub accounts, they won't have their own entry -> we have to include them here
	            entriesList = util.getEntriesFromAccountAndSubaccounts (session, a);
	        }

            entriesList = util.sortChronogicalyEntries(entriesList);
            // calculate the sum for each month
            for (int i=0; i<numberOfMonths; i++) {
                accountCategory.setLong(i, util.getSumMouvementsBetweenDates(entriesList, dateBegin[i], dateEnd[i]));
                accountCategory.orderingValue += accountCategory.getLong(i);
                accountCategory.totalValue += accountCategory.getLong(i);
            }
            // add an Entry for the "moyenne"
            accountCategory.setLong(numberOfMonths, accountCategory.totalValue / numberOfMonths);
            
            currentAccount ++ ;
            accounts.add(accountCategory);
            totalValue += accountCategory.totalValue; 
        }

        // Order them
        Collections.sort(accounts);
        Collections.reverse(accounts);
        
        // remove the last ones
        boolean maximumReached = false;
        while (!maximumReached) {
            accountCategory last = (accountCategory) accounts.get(accounts.size()-1);
            accountCategory antelast = (accountCategory) accounts.get(accounts.size()-2);
            if (last.totalValue + antelast.totalValue < (totalValue/10)) {
                antelast.add(last);
                accounts.remove(last);
            } else {
                maximumReached = true;
            }
        }
        
        // Cut the last name to avoid it to be too long
        {
            accountCategory last = (accountCategory) accounts.get(accounts.size()-1);
            if (last.name.length() > 120) last.name = last.name.substring(0,120) + "...";
        }
        
        // enter them in graph
        currentAccount = 0;
        itAccounts = accounts.iterator();
        while (itAccounts.hasNext()) {
            accountCategory a = (accountCategory) itAccounts.next();
            for (int i = 0; i < numberOfMonths; i++) {
                dataset.setValue(a.getLong(i),
                     a, new Month(dateBegin[i]));
            }
            // don't forget the middle value
            dataset.setValue(a.getLong(numberOfMonths), a, "Moyenne");

            currentAccount++;
        }



        return dataset;
    }

    private class accountCategory implements Comparable {
        
        String name;
        public long orderingValue;
        public Vector values;
        public long totalValue;
        
        public accountCategory(String name, long orderingValue) {
            this.name = name;
            this.orderingValue = orderingValue;
            values = new Vector(10,10);
            this.totalValue = orderingValue;
        }
        
        public int compareTo (Object o) {
            long thisVal = this.orderingValue;
            long anotherVal =  ((accountCategory) o).orderingValue;
            return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
        }
        
        public String toString () {
            return name;
        }
        
        public long getLong (int index) {
            return ((Long) values.get(index)).longValue();
        }
        
        public void setLong (int index, long value) {
            if (values.size()<=index) values.setSize(index+1);
            values.set(index, new Long(value));
        }
        
        /**
         * "Add" a category to the current one by summing their values and concacenate their names.
         * @param other
         */
        public void add (accountCategory other) {
            this.name = this.name + " + " + other.name;
            
            this.totalValue += other.totalValue;
            for (int i =0; i < this.values.size(); i++) {
                setLong(i, this.getLong(i) + other.getLong(i));
            }
        }

        
    }
}
