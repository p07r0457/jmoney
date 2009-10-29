/*
 * 
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.ofx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.ofx.model.OfxEntryInfo;
import net.sf.jmoney.ofx.parser.SimpleDOMParser;
import net.sf.jmoney.ofx.parser.SimpleElement;
import net.sf.jmoney.ofx.parser.TagNotFoundException;
import net.sf.jmoney.reconciliation.ReconciliationAccountInfo;
import net.sf.jmoney.reconciliation.utilities.ImportMatcher;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.model.StockEntry;
import net.sf.jmoney.stocks.model.StockEntryInfo;
import net.sf.jmoney.stocks.model.StockInfo;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class OfxImporter {

	private IWorkbenchWindow window;
	
	public OfxImporter(IWorkbenchWindow window) {
		this.window = window;
	}

	public void importFile(File file) {
		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog waitDialog = new MessageDialog(
					window.getShell(),
					"Disabled Action Selected",
					null, // accept the default window icon
					"You cannot import data into an accounting session unless you have a session open.  You must first open a session or create a new session.",
					MessageDialog.ERROR,
					new String[] { IDialogConstants.OK_LABEL }, 0);
			waitDialog.open();
			return;
		}

		try {
			/*
			 * Create a transaction to be used to import the entries.  This allows the entries to
			 * be more efficiently written to the back-end datastore and it also groups
			 * the entire import as a single change for undo/redo purposes.
			 */
			TransactionManager transactionManager = new TransactionManager(sessionManager);

			BufferedReader buffer = null;
			buffer = new BufferedReader(new FileReader(file));

			SimpleDOMParser parser = new SimpleDOMParser();
			SimpleElement rootElement = null;
			rootElement = parser.parse(buffer);
			
//			FileWriter fw = new FileWriter(new File("c:\\xml.xml"));
//			String xml = rootElement.toXMLString(0);
//			fw.append(xml);
//			fw.close();

			Session session = transactionManager.getSession();

			Session sessionOutsideTransaction = sessionManager.getSession();

			SimpleElement currencyElement = rootElement.findElement("CURDEF");
			Currency currency = null;
			if (currencyElement != null) {
				// found a default currency to use
				String currencyCode = currencyElement.getTrimmedText();
				currency = Currency.getInstance(currencyCode);
			}

			SimpleElement statementResultElement = rootElement.getDescendant("INVSTMTMSGSRSV1", "INVSTMTTRNRS", "INVSTMTRS");

			SimpleElement accountFromElement = statementResultElement.getDescendant("INVACCTFROM");
			String accountNumber = accountFromElement.getString("ACCTID");
			
			StockAccount account = null;
			StockAccount accountOutsideTransaction = null;
			for (Account eachAccount : sessionOutsideTransaction.getAccountCollection()) {
				if (eachAccount instanceof StockAccount) {
					StockAccount stockAccount= (StockAccount)eachAccount;
					if (accountNumber.equals(stockAccount.getAccountNumber())) {
						accountOutsideTransaction = stockAccount;
						account = transactionManager.getCopyInTransaction(accountOutsideTransaction);
					}
				}
			}
			
			if (account == null) {
				MessageDialog.openError(
						window.getShell(),
						"No Matching Account Found",
						"The OFX file contains data for brokerage account number " + accountNumber + ".  However no stock account exists with such an account number.  You probably need to set the account number property for the appropriate account.");
				return;
			}
			
			if (account.getDividendAccount() == null) {
				MessageDialog.openError(
						window.getShell(),
						"Account Not Configured",
						"The " + account.getName() + " account does not have an account set to hold the dividend payments.  Select the " + account.getName() + " from the Navigator view, then open the properties view and select a dividend account.");
				return;
			}
			
			if (account.getCommissionAccount() == null) {
				MessageDialog.openError(
						window.getShell(),
						"Account Not Configured",
						"The " + account.getName() + " account does not have an account set to hold the commission amounts.  Select the " + account.getName() + " from the Navigator view, then open the properties view and select a commission account.");
				return;
			}

			/*
			 * The tax 1 account is used for anything marked as 'fees'.
			 */
			if (account.getTax1Account() == null) {
				MessageDialog.openError(
						window.getShell(),
						"Account Not Configured",
						"The " + account.getName() + " account does not have an account set to hold the tax 1 payments.  Select the " + account.getName() + " from the Navigator view, then open the properties view and select a tax 1 account.");
				return;
			}
			
			/*
			 * We update our security list before importing the transactions.
			 * 
			 * We could do this afterwards and things would work fairly well.  The transaction
			 * import would create a stock object for each CUSIP, giving it a default name
			 * which is basically the CUSIP.  The the actual name and ticker symbol will be filled
			 * in when the securities list is imported.
			 * 
			 * However, when importing transactions, financial institutions often put the stock name
			 * or ticker symbol in the memo field.  This results in a duplication of information and
			 * also makes it harder to perform pattern matching on the memo field.  We therefore
			 * replace these with '<stock name>', '<ticker>', etc.  To do that, we need to know the
			 * stock name and ticker when we import the transactions.  Hence, to save a second pass
			 * through the transactions, we import the securities first. 
			 */
			SimpleElement secList = rootElement.getDescendant("SECLISTMSGSRSV1", "SECLIST");
			for (SimpleElement securityElement : secList.getChildElements()) {
				if (securityElement.getTagName().equals("STOCKINFO")
						|| securityElement.getTagName().equals("MFINFO")) {
					SimpleElement secInfoElement = securityElement.findElement("SECINFO");
					SimpleElement secIdElement = securityElement.findElement("SECID");

					String name = toTitleCase(secInfoElement.getString("SECNAME"));
					String symbol = secInfoElement.getString("TICKER");

					Stock stock = findStock(session, secIdElement);
					
					String defaultName = secIdElement.getString("UNIQUEIDTYPE") + ": " + secIdElement.getString("UNIQUEID");
					if (stock.getName().equals(defaultName)) {
						stock.setName(name);
					}
					
					if (stock.getSymbol() == null) {
						stock.setSymbol(symbol);
					}
				} else {
					System.out.println("unknown element in SECLIST");
					String elementXml = securityElement.toXMLString(0);
					System.out.println(elementXml);
				}
			}

			/*
			 * Get the set of ids that have already been imported
			 */
			Set<String> fitIds = new HashSet<String>();
			for (Entry entry : accountOutsideTransaction.getEntries()) {
				String fitId = entry.getPropertyValue(OfxEntryInfo.getFitidAccessor());
				if (fitId != null) {
					fitIds.add(fitId);
				}
			}
			
			SimpleElement transListElement = statementResultElement.getDescendant("INVTRANLIST");

			ImportMatcher matcher = new ImportMatcher(account.getExtension(ReconciliationAccountInfo.getPropertySet(), true));
			
			for (SimpleElement transactionElement : transListElement.getChildElements()) {
				if (transactionElement.getTagName().equals("DTSTART")) {
					// ignore
				} else if (transactionElement.getTagName().equals("DTEND")) {
					// ignore
				} else if (transactionElement.getTagName().equals("INVBANKTRAN")) {

					SimpleElement stmtTrnElement = transactionElement.findElement("STMTTRN");

					Date postedDate = stmtTrnElement.getDate("DTPOSTED");
					long amount = stmtTrnElement.getAmount("TRNAMT");
					String fitid = stmtTrnElement.getString("FITID");
					String memo = stmtTrnElement.getString("MEMO");
					
					String checkNumber = stmtTrnElement.getString("CHECKNUM");
					if (checkNumber != null) {
//						checkNumber = checkNumber.trim(); // Is this needed???
						// QFX (or at least hsabank.com) sets CHECKNUM to zero even though not a check.
						// This is probably a bug at HSA Bank, but we ignore check numbers of zero.
						if (checkNumber.equals("0")) {
							checkNumber = null;
						}
					}

					if (fitIds.contains(fitid)) {
						// This transaction has been previously imported.
						// We ignore it.
						continue;
					}

					/*
					 * First we try auto-matching.
					 * 
					 * If we have an auto-match then we don't have to create a new
					 * transaction at all. We just update a few properties in the
					 * existing entry.
					 * 
					 * An entry auto-matches if:
					 *  - The amount exactly matches
					 *  - The entry has no FITID set
					 *  - If a check number is specified in the existing entry then
					 * it must match a check number in the import (but if no check
					 * number is in the existing entry, that is ok)
					 *  - The date must be either exactly equal,

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
					for (Entry entry : accountOutsideTransaction.getEntries()) {
						if (entry.getPropertyValue(OfxEntryInfo.getFitidAccessor()) == null
								&& entry.getAmount() == amount) {
							System.out.println("amount: " + amount);
							if (entry.getCheck() == null) {
								if (entry.getTransaction().getDate().equals(postedDate)) {
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
									Calendar fiveDaysLater = Calendar.getInstance();
									fiveDaysLater.setTime(entry.getTransaction().getDate());
									fiveDaysLater.add(Calendar.DAY_OF_MONTH, 5);

									if ((checkNumber == null || checkNumber.length() == 0) 
											&& (postedDate.equals(entry.getTransaction().getDate())
													|| postedDate.after(entry.getTransaction().getDate()))
													&& postedDate.before(fiveDaysLater.getTime())) {
										// Auto-reconcile
										possibleMatches.add(entry);
									}
								}
							} else {
								// A check number is present
								Calendar twentyDaysLater = Calendar.getInstance();
								twentyDaysLater.setTime(entry.getTransaction().getDate());
								twentyDaysLater.add(Calendar.DAY_OF_MONTH, 20);

								if (entry.getCheck().equals(checkNumber)
										&& (postedDate.equals(entry.getTransaction().getDate())
												|| postedDate.after(entry.getTransaction().getDate()))
												&& postedDate.before(twentyDaysLater.getTime())) {
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

						Entry entryInTrans = transactionManager.getCopyInTransaction(match);
						entryInTrans.setValuta(postedDate);
						entryInTrans.setCheck(checkNumber);
						entryInTrans.setPropertyValue(OfxEntryInfo.getFitidAccessor(), fitid);

						continue;
					}

					/*
					 * No existing entry matches, either on FITID or by matching dates and amounts,
					 * so we need to create a new transaction.
					 */
					Transaction transaction = session.createTransaction();

					Entry firstEntry = transaction.createEntry();
					firstEntry.setAccount(account);

					firstEntry.setPropertyValue(OfxEntryInfo.getFitidAccessor(), fitid);

					transaction.setDate(postedDate);
					firstEntry.setValuta(postedDate);
					firstEntry.setAmount(amount);

					Entry otherEntry = transaction.createEntry();
					otherEntry.setAmount(-amount);
					
			   		/*
			   		 * Scan for a match in the patterns.  If a match is found,
			   		 * use the values for memo, description etc. from the pattern.
			   		 */
					String trnType = stmtTrnElement.getString("TRNTYPE");
					String textToMatch = MessageFormat.format(
							"TRNTYPE={0}\nMEMO={1}",
							trnType,
							memo);
					String defaultDescription = MessageFormat.format(
							"{0}: {1}",
							trnType.toLowerCase(),
							toTitleCase(memo));
					matcher.matchAndFill(textToMatch, firstEntry, otherEntry, toTitleCase(memo), defaultDescription);
				} else {
					// Assume a stock transaction

					SimpleElement invTransElement = transactionElement.findElement("INVTRAN");
					if (invTransElement == null) {
						String elementXml = transactionElement.toXMLString(0);
						System.out.println(elementXml);
						throw new RuntimeException("missing INVTRAN");
					}

					String fitid = invTransElement.getString("FITID");
					Date tradeDate = invTransElement.getDate("DTTRADE");
					Date settleDate = invTransElement.getDate("DTSETTLE");
					String memo = invTransElement.getString("MEMO");

					if (fitIds.contains(fitid)) {
						// This transaction has been previously imported.
						// We ignore it.
						continue;
					}

					// Create a new transaction
					Transaction transaction = session.createTransaction();

					Entry firstEntry = transaction.createEntry();
					firstEntry.setAccount(account);

					firstEntry.setPropertyValue(OfxEntryInfo.getFitidAccessor(), fitid);

					SimpleElement secIdElement = transactionElement.findElement("SECID");
					Stock stock = findStock(session, secIdElement);

					/*
					 * When importing transactions, financial institutions often put
					 * the stock name or ticker symbol in the memo field. This
					 * results in a duplication of information and also makes it
					 * harder to perform pattern matching on the memo field. We
					 * therefore replace these with '<stock name>', '<ticker>', etc.
					 */
					memo = memo.replace(stock.getName().toUpperCase(), "<stock name>");
					if (stock.getSymbol() != null) {
						memo.replace(stock.getSymbol(), "<ticker>");
					}
					if (stock.getCusip() != null) {
						memo.replace(stock.getCusip(), "<CUSIP>");
					}
					
					transaction.setDate(tradeDate);
					firstEntry.setValuta(settleDate);

					long total = transactionElement.getAmount("TOTAL");
					firstEntry.setAmount(total);

					firstEntry.setMemo(memo);

					if (transactionElement.getTagName().startsWith("BUY")
							|| transactionElement.getTagName().startsWith("SELL")) {

						String units = transactionElement.getString("UNITS");

						String unitPrice = transactionElement.getString("UNITPRICE");

						long commission = transactionElement.getAmount("COMMISSION", 0);
						if (commission != 0) {
							StockEntry commissionEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
							commissionEntry.setAccount(account.getCommissionAccount());
							commissionEntry.setAmount(commission);
							commissionEntry.setStock(stock);
						}

						long fees = transactionElement.getAmount("FEES", 0);
						if (fees != 0) {
							StockEntry feesEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
							feesEntry.setAccount(account.getTax1Account());
							feesEntry.setAmount(fees);
							feesEntry.setStock(stock);
						}

						if (units == null) {
							units = "1";   // TODO
						}
						Long quantity = stock.parse(units);

						StockEntry saleEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
						saleEntry.setAccount(account);

						if (transactionElement.getTagName().startsWith("BUY")) {
							saleEntry.setAmount(quantity);
						} else {
							// For sales, units are negative in the OFX file, so it's the same
							saleEntry.setAmount(quantity);
						}

						saleEntry.setStock(stock);
						saleEntry.setStockChange(true);



						if (transactionElement.getTagName().equals("BUYMF")) {
							// Mutual fund purchase
						} else if (transactionElement.getTagName().equals("BUYSTOCK")) {
							// Stock purchase
						} else if (transactionElement.getTagName().equals("SELLMF")) {
							// Mutual fund sale
						} else if (transactionElement.getTagName().equals("SELLSTOCK")) {
							// Stock sale
						} else {
							System.out.println("unknown element: " + transactionElement.getTagName());
							String elementXml = transactionElement.toXMLString(0);
							System.out.println(elementXml);
							throw new RuntimeException("unknown element: " + transactionElement.getTagName());
						}
					} else if (transactionElement.getTagName().equals("INCOME")) {
						String incomeType = transactionElement.getString("INCOMETYPE");
						if ("DIV".equals(incomeType)) {
							StockEntry dividendEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
							dividendEntry.setAccount(account.getDividendAccount());
							dividendEntry.setAmount(-total);
							dividendEntry.setMemo("divdend");
							dividendEntry.setStock(stock);
						} else if ("CGLONG".equals(incomeType)) {
							StockEntry dividendEntry = transaction.createEntry().getExtension(StockEntryInfo.getPropertySet(), true);
							dividendEntry.setAccount(account.getDividendAccount());
							dividendEntry.setAmount(-total);
							dividendEntry.setMemo("capitial gains distribution - long term");
							dividendEntry.setStock(stock);
						} else {
							/*
							 * It might be "INTEREST", "MISC" or perhaps some other
							 * value. We defer to the user entered patterns to
							 * categorize these.
							 */
							Entry otherEntry = transaction.createEntry();
							otherEntry.setAmount(-total);
							
							String textToMatch = MessageFormat.format(
									"INCOMETYPE={0}\nMEMO={1}",
									incomeType,
									memo);
							String defaultDescription = MessageFormat.format(
									"{0}: {1}",
									incomeType.toLowerCase(),
									toTitleCase(memo));
							matcher.matchAndFill(textToMatch, firstEntry, otherEntry, toTitleCase(memo), defaultDescription);
						}
					} else {
						System.out.println("unknown element: " + transactionElement.getTagName());
						String elementXml = transactionElement.toXMLString(0);
						System.out.println(elementXml);
						throw new RuntimeException("unknown element: " + transactionElement.getTagName());
					}
				}
			}

			/*
			 * All entries have been imported and all the properties
			 * have been set and should be in a valid state, so we
			 * can now commit the imported entries to the datastore.
			 */
			if (transactionManager.hasChanges()) {
			String transactionDescription = MessageFormat.format("Import {0}", file.getName());
			transactionManager.commit(transactionDescription);									

			StringBuffer combined = new StringBuffer();
			combined.append(file.getName());
			combined.append(" was successfully imported. ");
			MessageDialog.openInformation(window.getShell(), "OFX file imported", combined.toString());
			} else {
				MessageDialog.openWarning(window.getShell(), "OFX file not imported", 
						MessageFormat.format(
								"{0} was not imported because all the data in it had already been imported.", 
								file.getName()));
			}
		} catch (IOException e) {
			MessageDialog.openError(window.getShell(), "Unable to read OFX file", e.getLocalizedMessage());
		} catch (TagNotFoundException e) {
			MessageDialog.openError(window.getShell(), "Unable to read OFX file", e.getLocalizedMessage());
		}
	}

	private String toTitleCase(String text) {
		String lowerCaseText = text.toLowerCase();
		char[] charArray = lowerCaseText.toCharArray();

		Pattern pattern = Pattern.compile("\\b([a-z])");
		Matcher matcher = pattern.matcher(lowerCaseText);

		while(matcher.find()) {
			int index = matcher.end(1) - 1;
			charArray[index] = Character.toUpperCase(charArray[index]);
		}

		return new String(charArray);
	}

	private Stock findStock(Session session, SimpleElement secIdElement) {
		String uniqueId = secIdElement.getString("UNIQUEID");
		String uniqueIdType = secIdElement.getString("UNIQUEIDTYPE");
		
		ScalarPropertyAccessor<String> securityIdField = null;
		if ("CUSIP".equals(uniqueIdType)) {
			securityIdField = StockInfo.getCusipAccessor();
		} else {
			// We don't recognize the id type, so use the symbol field
			// and hope it does not conflict with another use of the
			// symbol field.
			securityIdField = StockInfo.getSymbolAccessor();
		}

		if (uniqueId.length() == 0) {
			throw new RuntimeException("can this ever happen?");
		}

		Stock stock = null;
		for (Commodity commodity : session.getCommodityCollection()) {
			if (commodity instanceof Stock) {
				Stock eachStock = (Stock)commodity;
				if (uniqueId.equals(eachStock.getPropertyValue(securityIdField))) {
					stock = eachStock;
					break;
				}
			}
		}

		if (stock == null) {
			// Create it.
			stock = session.createCommodity(StockInfo.getPropertySet());
			if (securityIdField != null) {
				stock.setPropertyValue(securityIdField, uniqueId);
			}
			
			/*
			 * The name and ticker should be set later when the SECLIST element
			 * is processed.  However just in case that does not happen, we set
			 * a name because we mustn't create securities with blank names.
			 */
			stock.setName(uniqueIdType + ": " + uniqueId);
		}

		return stock;
	}
}