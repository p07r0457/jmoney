package net.sf.jmoney.importer.matcher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;

import net.sf.jmoney.importer.model.MemoPattern;
import net.sf.jmoney.importer.model.PatternMatcherAccount;
import net.sf.jmoney.importer.model.ReconciliationEntryInfo;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;

public class ImportMatcher {

	private PatternMatcherAccount account;
	
	private List<MemoPattern> sortedPatterns;

	public ImportMatcher(PatternMatcherAccount account) {
		this.account = account;
		
		/*
		 * Get the patterns sorted into order.  It is important that we test patterns in the
		 * correct order because an entry may match both a general pattern and a more specific
		 * pattern.
		 */
		sortedPatterns = new ArrayList<MemoPattern>(account.getPatternCollection());
		Collections.sort(sortedPatterns, new Comparator<MemoPattern>(){
			public int compare(MemoPattern pattern1, MemoPattern pattern2) {
				return pattern1.getOrderingIndex() - pattern2.getOrderingIndex();
			}
		});
		
	}

	public void matchAndFill(String text, Entry entry1, Entry entry2, String defaultMemo, String defaultDescription) {
   		for (MemoPattern pattern: sortedPatterns) {
   			Matcher m = pattern.getCompiledPattern().matcher(text);
   			System.out.println(pattern.getPattern() + ", " + text);
   			if (m.matches()) {
   				/*
   				 * Group zero is the entire string and the groupCount method
   				 * does not include that group, so there is really one more group
   				 * than the number given by groupCount.
   				 */
   				Object [] args = new Object[m.groupCount()+1];
   				for (int i = 0; i <= m.groupCount(); i++) {
   					args[i] = m.group(i);
   				}
   				
   				// TODO: What effect does the locale have in the following?
   				if (pattern.getCheck() != null) {
   					entry1.setCheck(
   							new java.text.MessageFormat(
   									pattern.getCheck(), 
   									java.util.Locale.US)
   							.format(args));
   				}
   				
   				if (pattern.getMemo() != null) {
   					entry1.setMemo(
   							new java.text.MessageFormat(
   									pattern.getMemo(), 
   									java.util.Locale.US)
   							.format(args));
   				}
   				
   				if (pattern.getDescription() != null) {
       				entry2.setMemo(
       						new java.text.MessageFormat(
       								pattern.getDescription(), 
       								java.util.Locale.US)
       								.format(args));
   				}
   				
           		entry2.setAccount(pattern.getAccount());
           		
           		break;
   			}
   		}
   		
		/*
		 * If nothing matched, set the default account, the memo, and the
		 * description (the memo in the other account) but no other property.
		 */
   		if (entry2.getAccount() == null) {
   			entry2.setAccount(account.getDefaultCategory());
			entry1.setMemo(defaultMemo);
			entry2.setMemo(defaultDescription);
   		}
	}

	/**
	 * 
	 * @param entryData
	 * @param transactionManager
	 * @param session
	 * @param statement
	 * @return the entry for this transaction.
	 */
	public Entry process(net.sf.jmoney.importer.matcher.EntryData entryData, Session session) {
		/*
		 * First we try auto-matching.
		 * 
		 * If we have an auto-match then we don't have to create a new
		 * transaction at all. We just update a few properties in the
		 * existing entry.
		 * 
		 * An entry auto-matches if:
		 *  - The amount exactly matches
		 *  - The entry has no unique id set
		 *  - If a check number is specified in the existing entry then
		 * it must match a check number in the import (but if no check
		 * number is in the existing entry, that is ok)
		 *  - The date must be either exactly equal,
		 * 
		 * or it can be up to 10 days in the future but it can only be
		 * in the future if there is a check number match. This allows,
		 * say, a check to match that is likely not going to appear till
		 * a few days later.
		 * 
		 * or it can be up to 1 day in the future but only if there
		 * are no other entries that match. This restriction prevents a
		 * false match when there are lots of charges for the same
		 * amount very close together (e.g. consider a cup of coffee
		 * charged every day or two)
		 */
		Collection<Entry> possibleMatches = new ArrayList<Entry>();
		for (Entry entry : account.getBaseObject().getEntries()) {
			if (entry.getPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor()) == null
					&& entry.getAmount() == entryData.amount) {
				System.out.println("amount: " + entryData.amount);
				Date importedDate = (entryData.valueDate != null)
				? entryData.valueDate
						: entryData.clearedDate;
				if (entry.getCheck() == null) {
					if (entry.getTransaction().getDate().equals(importedDate)) {
						// Auto-reconcile
						possibleMatches.add(entry);
						
						/*
						 * Date exactly matched - so we can quit
						 * searching for other matches. (If user entered
						 * multiple entries with same check number then
						 * the user will not be surprised to see an
						 * arbitrary one being used for the match).
						 */
						break;
					} else {
						Calendar fiveDaysLater = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						fiveDaysLater.setTime(entry.getTransaction().getDate());
						fiveDaysLater.add(Calendar.DAY_OF_MONTH, 5);
						
						if ((entryData.check == null || entryData.check.length() == 0) 
								&& (importedDate.equals(entry.getTransaction().getDate())
								 || importedDate.after(entry.getTransaction().getDate()))
								 && importedDate.before(fiveDaysLater.getTime())) {
							// Auto-reconcile
							possibleMatches.add(entry);
						}
					}
				} else {
					// A check number is present
					Calendar twentyDaysLater = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
					twentyDaysLater.setTime(entry.getTransaction().getDate());
					twentyDaysLater.add(Calendar.DAY_OF_MONTH, 20);
					
					if (entry.getCheck().equals(entryData.check)
							&& (importedDate.equals(entry.getTransaction().getDate())
							 || importedDate.after(entry.getTransaction().getDate()))
							 && importedDate.before(twentyDaysLater.getTime())) {
						// Auto-reconcile
						possibleMatches.add(entry);
						
						/*
						 * Check number matched - so we can quit
						 * searching for other matches. (If user entered
						 * multiple entries with same check number then
						 * the user will not be surprised to see an
						 * arbitrary one being used for the match).
						 */
						break;
					}
				}
			}
		}

		if (possibleMatches.size() == 1) {
			Entry match = possibleMatches.iterator().next();
			
			// No, this matcher is not involved with transactions.
			// It can work in or outside one.
//			Entry entryInTrans = transactionManager.getCopyInTransaction(match);

			if (entryData.valueDate == null) {
				match.setValuta(entryData.clearedDate);
			} else {
				match.setValuta(entryData.valueDate);
			}

			match.setCheck(entryData.check);
			match.setPropertyValue(ReconciliationEntryInfo.getUniqueIdAccessor(), entryData.uniqueId);

			return match;
		}
		
   		Transaction transaction = session.createTransaction();
   		Entry entry1 = transaction.createEntry();
   		Entry entry2 = transaction.createEntry();
   		entry1.setAccount(account.getBaseObject());
   		
   		/*
   		 * Scan for a match in the patterns.  If a match is found,
   		 * use the values for memo, description etc. from the pattern.
   		 */
		String text = entryData.getTextToMatch();
		matchAndFill(text, entry1, entry2, entryData.getDefaultMemo(), entryData.getDefaultDescription());
		
   		entryData.assignPropertyValues(transaction, entry1, entry2);
   		
   		return entry1;
	}
}
