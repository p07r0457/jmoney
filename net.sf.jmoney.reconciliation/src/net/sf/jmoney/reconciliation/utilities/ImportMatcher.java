package net.sf.jmoney.reconciliation.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.reconciliation.MemoPattern;
import net.sf.jmoney.reconciliation.ReconciliationAccount;

public class ImportMatcher {

	private ReconciliationAccount account;
	
	private List<MemoPattern> sortedPatterns;
	
	public ImportMatcher(ReconciliationAccount account) {
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
}
