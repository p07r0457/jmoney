package net.sf.jmoney.charts;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;

import org.jfree.data.CategoryDataset;
import org.jfree.data.DefaultCategoryDataset;
import org.jfree.data.time.Month;
import org.jfree.data.time.RegularTimePeriod;

public class StackedAccountChart extends StackedChart {

    public StackedAccountChart(String title, Session session, StackedChartParameters params) {
        super(title, session, params);
    }

    protected CategoryDataset createValues(Session session) {
        long saldo = 0;
        Date[] dates;
        int numberOfAccounts = 0;
        int numberOfPeriods;
        List listAccounts;
        long totalValue = 0;
        
        params.instanciateConstructor();
        
        // Calculate the number of Periods
        numberOfPeriods = 0;
        for (RegularTimePeriod period = params.createPeriod(params.fromDate);
             period.getFirstMillisecond() < params.toDate.getTime();
             period = period.next()) {
            
            numberOfPeriods++;
        }
        
        // Calculate the begin and the end of each Time-Gap
        Date [] dateBegin = new Date[numberOfPeriods];
        Date [] dateEnd = new Date[numberOfPeriods];
        String[] dateLabels = new String[numberOfPeriods];
        RegularTimePeriod period = params.createPeriod(params.fromDate);
        for (int i=0; i<numberOfPeriods; i++) {
            dateBegin[i]= new Date(period.getFirstMillisecond());
            dateEnd[i]  = new Date(period.getLastMillisecond());
            period = period.next();
        }
        for (int i=0; i<numberOfPeriods; i++) 
            dateLabels[i] = params.dateformat.format(dateBegin[i]);
        
        
        // Get the accounts
        listAccounts = new LinkedList();
        for (int i=0; i<params.accountList.size(); i++) {
            CapitalAccount acc = (CapitalAccount) Util.getAccountByFullName(session, (String) params.accountList.get(i));
            listAccounts.add(acc);
            listAccounts.addAll(
                    Util.getSubAccountsUntilLevel(acc, params.maxLevel)
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
            boolean includeSubAccounts;
            if (a.getLevel() < params.maxLevel) {
	        	// If the account has sub accounts, they will have their own entry -> we don't have to include them here
	            includeSubAccounts = false;
	        } else {
	        	// If the account has sub accounts, they won't have their own entry -> we have to include them here
	            includeSubAccounts = true;
	        }
            
            long [] totals = Util.getEntryTotalsByPeriod(a,params.createPeriod(params.fromDate), numberOfPeriods, includeSubAccounts); 
	        
            // calculate the sum for each period
            for (int i=0; i<numberOfPeriods; i++) {
                accountCategory.setLong(i, totals[i]);
                accountCategory.orderingValue += accountCategory.getLong(i);
                accountCategory.totalValue += accountCategory.getLong(i);
            }
            // add an Entry for the "average"
            accountCategory.setLong(numberOfPeriods, accountCategory.totalValue / numberOfPeriods);
            
            currentAccount ++ ;
            accounts.add(accountCategory);
            totalValue += accountCategory.totalValue; 
        }

        // Order them
        Collections.sort(accounts);
        Collections.reverse(accounts);
        
        // remove the last ones
        boolean maximumReached = false;
        while (!maximumReached && accounts.size()>2) {
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
            for (int i = 0; i < numberOfPeriods; i++) {
                dataset.setValue(a.getLong(i),
                     a, new Month(dateBegin[i]));
            }
            // don't forget the middle value
            dataset.setValue(a.getLong(numberOfPeriods), a, "Moyenne");

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
