/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package net.sf.jmoney.qif;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.IDialogConstants;

import net.sf.jmoney.model2.*;

/**
 * Quicken interchange format file import and export.
 */
public class QIF implements FileFormat {
    static NumberFormat number = NumberFormat.getInstance(Locale.US);
    static Calendar calendar = Calendar.getInstance(Locale.US);
    IWorkbenchWindow window;

    /**
     * Creates a new QIF.
     */
    public QIF(IWorkbenchWindow window) {
        this.window = window;
        number.setMinimumFractionDigits(2);
        number.setMaximumFractionDigits(2);
    }
    
    /**
     * Imports a QIF-file.
     */
    public void importFile(Session session, File qifFile) {
        try {
            BufferedReader buffer = new BufferedReader(new FileReader(qifFile));
            String header = buffer.readLine();

            // import transactions of a non-investment account
            if (header.startsWith("!Type:Bank")
                || header.startsWith("!Type:Cash")
                || header.startsWith("!Type:Bar")
                || // MS M*ney97 german edition
            header.startsWith("!Type:CCard")
                || header.startsWith("!Type:Oth A")
                || header.startsWith("!Type:Oth L")) {

                String info =
                    QIFPlugin.getResourceString("QIF.chooseAccount")
                        + " \""
                        + qifFile.getName()
                        + "\".";
    		    AccountChooser accountChooser = new AccountChooser(window.getShell(), info);
/*
    		    int s =
                    accountChooser.showDialog(
                        session.getAccountIterator(),
                        info,
                        true);
                if (s == Constants.OK) {
                    // an existing account has been selected
                    importAccount(
                        session,
                        accountChooser.getSelectedAccount(),
                        buffer);
                } else if (s == Constants.NEW) {
                    // create new account to import transactions
                    String name = qifFile.getName();
                    if (name.endsWith(".qif"))
                        name = name.substring(0, name.length() - 4);
                    importAccount(
                        session,
                        getNewAccount(session, name),
                        buffer);
                }
*/                
            }

            // import transactions of a investment account
            else if (header.equals("!Type:Invst")) {
                System.err.println(
                    "QIF: Import of investment accounts is not suported.");
            }

            // import account list
            else if (header.equals("!CapitalAccount")) {
                System.err.println(
                    "QIF: Import of account lists is not supported.");
            }

            // import category list
            else if (header.equals("!Type:Cat")) {
                System.err.println(
                    "QIF: Import of category lists is not supported.");
            }

            // import class list
            else if (header.equals("!Type:Class")) {
                System.err.println(
                    "QIF: Import of class lists is not supported.");
            }

            // import memorized transaction list
            else if (header.equals("!Type:Memorized")) {
                System.err.println(
                    "QIF: Import of memorized transaction lists is not supported.");
            }
        } catch (IOException e) {
	        MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						QIFPlugin.getResourceString("MainFrame.FileError"), 
						null, // accept the default window icon
						QIFPlugin.getResourceString("MainFrame.CouldNotReadFile")
			            	+ " "
							+ qifFile.getPath(),
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
	        waitDialog.open();
        }  /* catch (CanceledException e) {} */  // TODO put this back
    }

    /**
     * Imports an account from a QIF-file.
     */
    private void importAccount(
        Session session,
        CapitalAccount account,
        BufferedReader buffer)
        throws IOException, CanceledException {
        String line;
        Accounts accounts = new Accounts();

        // This is in our plug-in so it should be
        // impossible for it to not be there.
        // TODO: is there a way of getting it directly,
        // rather than looking up by name, so no
        // exception is possible?
        PropertySet qifPropertySet;
        try {
        	qifPropertySet = PropertySet.getPropertySet("net.sf.jmoney.qif.entryProperties");
        } catch (PropertySetNotFoundException e) {
        	throw new RuntimeException("bad error in QIF");
        }
        
        while (true) {
            MutableTransaxion transaction = session.createNewTransaxion();
            Entry entry = transaction.createEntry();
            QIFEntry ourEntry = (QIFEntry)entry.getExtension(qifPropertySet);
            
            Account category = null;
            
            while (true) {
                line = buffer.readLine();
                if (line == null || line.equals("^"))
                    break;
                char firstChar = line.charAt(0);
                switch (firstChar) {
                    case 'D' :
                        transaction.setDate(parseDate(line));
                        break;
                    case 'T' :
                        extractAmount(
                            entry,
                            line,
                            account.getCurrency().getScaleFactor());
                        break;
                    case 'C' :
                        extractStatus(ourEntry, line);
                        break;
                    case 'N' :
                        entry.setCheck(line.substring(1));
                        break;
                    case 'P' :
                        entry.setDescription(line.substring(1));
                        break;
                    case 'L' :
/*                        
                        entry =
                            extractCategory(
                                session,
                                accounts,
                                account,
                                entry,
                                line);
 */
                        // We save the category and use it later to set
                        // up the transaction when we have read all the information
                        // for this entry.
                        category =
                            extractCategory(
                                session,
                                accounts,
                                line);
                        break;
                    case 'M' :
                        entry.setMemo(line.substring(1));
                        break;
                    case 'S' :
                        // This is assumed to be the first entry for each
                        // sub-entry in the split entry.  Create an extra
                        // entry now.
                        entry = transaction.createEntry();

                            extractSplittedCategory(
                                session,
                                accounts,
                                entry,
                                line);
                        break;
                    case 'E' :
                        extractSplittedDescription(entry, line);
                        break;
                    case '$' :
                            extractSplittedAmount(
                                entry,
                                line,
                                account.getCurrency().getScaleFactor());
                        break;
                    default :
                        break;
                }
            }
            if (line == null)
                break;
            
            if (category != null) {
                Entry otherEntry = transaction.createEntry();
                otherEntry.setAccount(category);
                otherEntry.setAmount(-entry.getAmount());
            }
            
            transaction.commit();
//          account.addEntry(entry);
            
            removeSimilarTransfer(session, entry);
        }
        
//      account.setEntries(account.getEntries()); // notify listeners
    }

/*    
    private SplittedEntry extractSplittedCategory(
        Session session,
        Accounts accounts,
        CapitalAccount account,
        Entry entry,
        String line)
        throws CanceledException {
        if (entry instanceof DoubleEntry) {
            DoubleEntry de = (DoubleEntry) entry;
            CapitalAccount other = (CapitalAccount) de.getCategory();
            other.getEntries().removeElement(de.getOther());
        }
        SplittedEntry se = entry.toSplittedEntry();
        se.setCategory(
            (Category) session.getCategories().getSplitNode().getUserObject());
        se.addEntry(
            extractCategory(session, accounts, account, new Entry(), line));
        return se;
    }
*/
    private void extractSplittedCategory(
        Session session,
        Accounts accounts,
        Entry entry,
        String line)
        throws CanceledException {
        entry.setAccount(extractCategory(session, accounts, line));  
    }
    
/*
    private SplittedEntry extractSplittedDescription(
        Entry entry,
        String line) {
        SplittedEntry se = toSplittedEntry(entry);
        ((Entry) se.getEntries().lastElement()).setDescription(
            line.substring(1));
        return se;
    }
*/
    private void extractSplittedDescription(
        Entry entry,
        String line) {
        entry.setDescription(line.substring(1));
    }

/*
    private SplittedEntry extractSplittedAmount(
        Entry entry,
        String line,
        short factor)
        throws CanceledException {
        SplittedEntry se = toSplittedEntry(entry);
        Entry e = (Entry) se.getEntries().lastElement();
        extractAmount(e, line, factor);
        if (e instanceof DoubleEntry)
             ((DoubleEntry) e).getOther().setAmount(-e.getAmount());
        return se;
    }
*/
    private void extractSplittedAmount(
        Entry entry,
        String line,
        short factor)
        throws CanceledException {
        extractAmount(entry, line, factor);
    }

/*    
    private SplittedEntry toSplittedEntry(Entry entry) {
        SplittedEntry se;
        if (entry instanceof SplittedEntry) {
            se = (SplittedEntry) entry;
        } else {
            se = entry.toSplittedEntry();
            se.addEntry(new Entry());
        }
        return se;
    }
*/
    
    private void extractAmount(Entry entry, String line, short factor) {
        Number n = number.parse(line, new ParsePosition(1));
        entry.setAmount(n == null ? 0 : Math.round(n.doubleValue() * factor));
    }

    private void extractStatus(QIFEntry entry, String line) {
        char c = line.charAt(1);
/*
   	net.sf.jmoney.reconciliation.EntryExtension
            reconciliationEntry = (net.sf.jmoney.reconciliation.EntryExtension)
                        entry.getExtension("net.sf.jmoney.reconciliation");

        if (c == 'x' || c == 'X')
            reconciliationEntry.setStatus(net.sf.jmoney.reconciliation.EntryExtension.CLEARED);
        else if (c == '*')
            reconciliationEntry.setStatus(net.sf.jmoney.reconciliation.EntryExtension.RECONCILING);

 entry.setCharacterPropertyValue("net.sf.jmoney.QIF.reconciliationStatus", c);
  */      
        if (c == 'x' || c == 'X' || c == '*') {
            entry.setReconcilingState(c);
        }
    }

    /**
     * Extract the category from the given input line and set this into
     * the given entry.
     */
/*
    private Entry extractCategory(
        Session session,
        Accounts accounts,
        CapitalAccount account,
        Entry entry,
        String line)
        throws CanceledException {
        if (line.charAt(1) == '[') {
            // transfer
            String accountName = line.substring(2, line.length() - 1);
            CapitalAccount other = accounts.getAccount(accountName, session);
            if (account != other) {
                DoubleEntry doubleEntry = entry.toDoubleEntry();
                doubleEntry.setCategory(other);
                doubleEntry.getOther().setCategory(account);
                entry = doubleEntry;
                other.addEntry(doubleEntry.getOther());
            }
        } else {
            // assumption: a category consists at least of one char
            // either "LCategory" or "LCategory:Subcategory"
            int colon;
            for (colon = 1; colon < line.length(); colon++)
                if (line.charAt(colon) == ':')
                    break;
            if (colon == line.length()) {
                // "LCategory"
                String categoryName = line.substring(1);
                entry.setCategory(getCategory(categoryName, session));
            } else {
                // "LCategory:Subcategory
                String categoryName = line.substring(1, colon);
                String subcategoryName = line.substring(colon + 1);
                IncomeExpenseAccount category = getCategory(categoryName, session);
                entry.setCategory(
                    getSubcategory(subcategoryName, category, session));
            }
        }
        return entry;
    }
 */
    private Account extractCategory(
        Session session,
        Accounts accounts,
        String line)
        throws CanceledException {
        if (line.charAt(1) == '[') {
            // transfer
            String accountName = line.substring(2, line.length() - 1);
            return accounts.getAccount(accountName, session);
        } else {
            // assumption: a category consists at least of one char
            // either "LCategory" or "LCategory:Subcategory"
            int colon;
            for (colon = 1; colon < line.length(); colon++)
                if (line.charAt(colon) == ':')
                    break;
            if (colon == line.length()) {
                // "LCategory"
                String categoryName = line.substring(1);
                return getCategory(categoryName, session);
            } else {
                // "LCategory:Subcategory
                String categoryName = line.substring(1, colon);
                String subcategoryName = line.substring(colon + 1);
                IncomeExpenseAccount category = getCategory(categoryName, session);
                return getSubcategory(subcategoryName, category, session);
            }
        }
    }

    public void exportAccount(Session session, CapitalAccount account, File file) {
        PropertySet qifPropertySet;
        try {
        	qifPropertySet = PropertySet.getPropertySet("net.sf.jmoney.qif.entryProperties");
        } catch (PropertySetNotFoundException e) {
        	throw new RuntimeException("bad error in QIF");
        }
        
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            Iterator entryIter = account.getEntriesIterator(session);

            // TODO: We need to ensure the entries are ordered in date order.
            // This was not done in the original JMoney.  One symptom of this
            // is that the date of the first entry containing the start balance
            // may be incorrect.
            
            // write header
            writeln(writer, "!Type:Bank");

            // write first entry (containing the start balance)
            if (entryIter.hasNext()) {
                Entry entry = (Entry) entryIter.next();
                String dateString = formatDate(entry.getTransaxion().getDate());
                if (dateString != null)
                    writeln(writer, dateString);
            }
            writeln(
                writer,
                "T" + formatAmount(account.getStartBalance(), account));
            writeln(writer, "CX");
            writeln(writer, "POpening Balance");
            writeln(writer, "L[" + account.getName() + "]");
            writeln(writer, "^");

            // write entries
            for (entryIter = account.getEntriesIterator(session); entryIter.hasNext(); ) {
                Entry entry = (Entry) entryIter.next();
                // date
                String dateString = formatDate(entry.getTransaxion().getDate());
                if (dateString != null)
                    writeln(writer, dateString);
                // memo
                if (entry.getMemo() != null)
                    writeln(writer, "M" + entry.getMemo());

                // status
                QIFEntry ourEntry = (QIFEntry)entry.getExtension(qifPropertySet);
                if (ourEntry.getReconcilingState() == '*')
                    writeln(writer, "C*");
                else if (ourEntry.getReconcilingState() == 'X')
                    writeln(writer, "CX");

                // amount
                writeln(writer, "T" + formatAmount(entry.getAmount(), account));
                // check
                if (entry.getCheck() != null)
                    writeln(writer, "N" + entry.getCheck());
                // description
                if (entry.getDescription() != null)
                    writeln(writer, "P" + entry.getDescription());
                // category
                Account category = entry.getAccount();
                if (category != null) {
                    if (category instanceof CapitalAccount)
                        writeln(
                            writer,
                            "L[" + category.getName() + "]");
                    else {
                        writeln(writer, "L" + category.getFullAccountName());
                    }
                    // TODO: Split Entries
                }
                // end of entry
                writeln(writer, "^");
            }
            writer.close();
        } catch (IOException e) {
	        MessageDialog waitDialog =
				new MessageDialog(
						window.getShell(), 
						QIFPlugin.getResourceString("MainFrame.FileError"), 
						null, // accept the default window icon
						QIFPlugin.getResourceString("MainFrame.CouldNotWriteFile")
			            	+ " "
							+ file.getPath(),
						MessageDialog.ERROR, 
						new String[] { IDialogConstants.OK_LABEL }, 0);
	        waitDialog.open();
        }
    }

    private String formatAmount(long amount, CapitalAccount account) {
        return number.format(
            ((double) amount) / account.getCurrency().getScaleFactor());
    }

    /**
     * Parses the date string and returns a date object:
     *   11/2/98 ->> 11/2/1998
     *   3/15'00 ->> 3/15/2000
     */
    private Date parseDate(String line) {
        try {
            StringTokenizer st = new StringTokenizer(line, "D/\'");
            int month = Integer.parseInt(st.nextToken().trim());
            int day = Integer.parseInt(st.nextToken().trim());
            int year = Integer.parseInt(st.nextToken().trim());
            if (year < 100) {
                if (line.indexOf("'") < 0)
                    year = year + 1900;
                else
                    year = year + 2000;
            }
            calendar.clear();
            calendar.set(year, month - 1, day);
            return calendar.getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String formatDate(Date date) {
        if (date == null)
            return null;
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);
        if ((year >= 1900) && (year < 2000))
            return "D" + month + "/" + day + "/" + (year - 1900);
        if ((year >= 2000) && (year < 2010))
            return "D" + month + "/" + day + "\'0" + (year - 2000);
        else if ((year >= 2010) && (year < 2100))
            return "D" + month + "/" + day + "\'" + (year - 2000);
        else
            return null;
    }

    private CapitalAccount getNewAccount(Session session, String accountName) {
        MutableCapitalAccount account = session.createNewCapitalAccount();
        account.setName(accountName);
        return account.commit();
    }

    /**
     * Returns the category with the specified name. If it doesn't exist a new
     * category will be created.
     */
    // TODO: How can we make this thread safe???
    private IncomeExpenseAccount getCategory(String categoryName, Session session) {
        IncomeExpenseAccount category =
            searchCategory(categoryName, session.getIncomeExpenseAccountIterator());
        if (category == null) {
            MutableIncomeExpenseAccount mutableCategory = session.createNewIncomeExpenseAccount();
            mutableCategory.setName(categoryName);
            category = mutableCategory.commit();
        }
        return category;
    }

    /**
     * Returns the subcategory with the specified name. If it doesn't exist a new
     * subcategory will be created.
     */
    private IncomeExpenseAccount getSubcategory(
    String name,
    IncomeExpenseAccount category,
    Session session) {
        IncomeExpenseAccount subcategory =
        searchCategory(name, category.getSubAccountIterator());

        // Another thread may have deleted the category.  To be sure,
        // we should lock the category so that it cannot be deleted.
        // If, when we attempt to lock it, we find we are already too
        // late then we continue to create a new category as though
        // no category was found.
/* database model is not yet threadsafe
        try {
            subcategory.lockExistence();
        } catch (ObjectDeletedException e) {
            subcategory = null;
        }
*/        
        if (subcategory == null) {
/*            
            MutableCategory newSubCategory;

            // Multiple threads cannot be allowed to create a new sub-category
            // of a given category at the same time because then there would be
            // no way of ensuring that the sub-category names are unique.
            
            // When we get a mutable sub-category for the category, we indicate
            // that we are holding this short term.  (No user input occurs
            // while we are holding the object for update).
            
            do {
                try {
                    newSubCategory = category.createNewSubCategory(false, getClass());
                } catch (ObjectLockedException e) {
                    // Someone else is creating a new sub-category to this category
                    // but has not yet committed it.
                    // We should wait and retry.
                    yield();
                    continue;
                }
            } while (false);

            // Search again
            // because it is possible that the other thread was adding
            // a category of the same name.  In that situation, we must
            // be sure we cannot have a race condition which results
            // in two categories of the same name.
            subcategory =
            searchCategory(name, category.getSubCategoryIterator());
            
            if (subcategory == null) {
                newSubCategory.setName(name);
                subCategory = newSubCategory.commit();
            } else {
                newSubCategory.rollback();
            }
*/            
            MutableIncomeExpenseAccount newSubCategory = category.createNewSubAccount(session);
            newSubCategory.setName(name);
            subcategory = newSubCategory.commit();
        }
        
        return subcategory;
        
        // The category returned by this function may be locked.
        // This ensures that no one else can delete it.
        // Once a reference to the category has been committed to
        // the database, the lock should be removed.
    }

    /**
     * Check if the transfer already exists.
     */
/*
    private void removeSimilarTransfer(CapitalAccount a1, Entry newEntry) {
        if (newEntry instanceof SplittedEntry) {
            SplittedEntry se = (SplittedEntry) newEntry;
            for (int i = 0; i < se.getEntries().size(); i++) {
                Entry subEntry = (Entry) se.getEntries().elementAt(i);
                if (subEntry instanceof DoubleEntry) {
                    DoubleEntry newDe1 = (DoubleEntry) subEntry;
                    DoubleEntry newDe2 = newDe1.getOther();
                    CapitalAccount a2 = (CapitalAccount) newDe1.getCategory();
                    for (int j = 0; j < a2.getEntries().size(); j++) {
                        Entry oldE2 = (Entry) a2.getEntries().elementAt(j);
                        if ((newDe2 != oldE2)
                            && (oldE2 instanceof DoubleEntry)
                            && (newDe2.getAmount() == oldE2.getAmount())
                            && equals(newDe2.getDate(), oldE2.getDate())
                            && equals(
                                newDe2.getCategory(),
                                oldE2.getCategory())) {
                            DoubleEntry oldDe2 = (DoubleEntry) oldE2;
                            a1.getEntries().removeElement(oldDe2.getOther());
                            a2.getEntries().removeElement(oldDe2);
                        }
                    }
                }
            }
        } else if (newEntry instanceof DoubleEntry) {
            DoubleEntry newDe = (DoubleEntry) newEntry;
            for (int i = 0; i < a1.getEntries().size(); i++) {
                Entry e = (Entry) a1.getEntries().elementAt(i);
                if ((newDe != e)
                    && (e instanceof DoubleEntry)
                    && equals(newDe.getCategory(), e.getCategory())
                    && (newDe.getAmount() == e.getAmount())
                    && equals(newDe.getDate(), e.getDate())
                    && equals(newDe.getCheck(), e.getCheck())
                    && equals(newDe.getDescription(), e.getDescription())
                    && equals(newDe.getMemo(), e.getMemo()))
                    newDe.remove();
            }
        }
    }
 */
    private void removeSimilarTransfer(Session session, Entry newEntry) {
        CapitalAccount account = (CapitalAccount)newEntry.getAccount();
        for (Iterator iter = account.getEntriesIterator(session); iter.hasNext(); ) {
            Entry entry2 = (Entry) iter.next();
            if ((entry2 != newEntry)
            && (entry2.getAmount() == newEntry.getAmount())
            && (entry2.getTransaxion().equals(newEntry.getTransaxion()))) {
                session.removeTransaxion(entry2.getTransaxion());
            }
        }
    }

    private boolean equals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null)
            return true;
        if (obj1 == null || obj2 == null)
            return false;
        return obj1.equals(obj2);
    }

    /**
     * Searches a category and returns null if it doesn't exist.
     */
    private IncomeExpenseAccount searchCategory(String name, Iterator categoryIterator) {
        while (categoryIterator.hasNext()) {
            Account obj = (Account) categoryIterator.next();
            if (obj instanceof IncomeExpenseAccount) {
                IncomeExpenseAccount category = (IncomeExpenseAccount) obj;
                if (category.getName().equals(name))
                    return category;
            }
        }
        return null;
    }

    /**
     * Writes a line and jumps to a new one.
     */
    private void writeln(BufferedWriter writer, String line)
        throws IOException {
        writer.write(line);
        writer.newLine();
    }

    /**
     * Auxiliary class with the function of a table with the columns "names"
     * and "accounts".
     * Provides a method "getAccount()" which returns the corresponding account to
     * a given account name. If there is no entry in the table the user will be
     * asked to choose an account.
     */
    class Accounts {
        Vector names = new Vector(10);
        Vector accounts = new Vector(10);
        CapitalAccount getAccount(String accountName, Session session)
            throws CanceledException {
            CapitalAccount account;
            int index;
            // search account
            for (index = 0; index < names.size(); index++) {
                String name = (String) names.get(index);
                if (name.equals(accountName))
                    break;
            }
            if (index == names.size()) {
                // account doesn't exist -> ask user
                String info =
                	QIFPlugin.getResourceString("QIF.chooseTransferAccount")
                        + " \""
                        + accountName
                        + "\".";
    		    AccountChooser accountChooser = new AccountChooser(window.getShell(), info);
    		    accountChooser.open();
 /*
                int status =
                    accountChooser.showDialog(
                        session.getAccountIterator(),
                        info,
                        true);
                if (status == Constants.OK)
                    account = accountChooser.getSelectedAccount();
                else if (status == Constants.NEW)
                    account = getNewAccount(session, accountName);
                else
                    throw new CanceledException("QIF import canceled.");
                names.add(accountName);
                accounts.add(account);
*/               
    		    
    		    account = null; // TODO remove this temp line
            } else {
                account = (CapitalAccount) accounts.get(index);
            }
            return account;
        }
    }

    /**
     * Exception to cancel an import/export operation.
     */
    public class CanceledException extends Exception {
        public CanceledException() {
            super();
        }
        public CanceledException(String s) {
            super(s);
        }
    }
}
