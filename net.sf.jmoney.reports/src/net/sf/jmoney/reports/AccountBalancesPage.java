/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.reports;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertyNotFoundException;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.jasperassistant.designer.viewer.ViewerComposite;

/**
 * @author Nigel Westbury
 */
public class AccountBalancesPage implements IBookkeepingPageFactory {

    private static final String PAGE_ID = "net.sf.jmoney.reports.accountBalances";
    
	public static final int ALL_ENTRIES = 0;

	public static final int CLEARED_ENTRIES = 1;

	public static final int DATE = 2;

	public static int[] filterIndexes;

	public static String[] filters;

	public static PropertyAccessor reconciliationStatusAccessor;
	
	static {
		try {
			reconciliationStatusAccessor = PropertySet.getPropertyAccessor("net.sf.jmoney.reconciliation.entryProperties.status");
		} catch (PropertyNotFoundException e) {
			// The reconciliation plug-in is not installed.
			// This does not stop operation of this plug-in.  We just have
			// to remove any operations that require the property from the plug-in.
			reconciliationStatusAccessor = null;
		}
		
		if (reconciliationStatusAccessor != null) {
			filterIndexes = new int[] {
				ALL_ENTRIES,
				CLEARED_ENTRIES,
				DATE,
			};
			filters = new String[] {
					ReportsPlugin.getResourceString("Report.AccountBalances.AllEntries"),
					ReportsPlugin.getResourceString("Report.AccountBalances.ClearedEntries"),
					ReportsPlugin.getResourceString("Entry.date"),
			};
		} else {
			filterIndexes = new int[] {
					ALL_ENTRIES,
					DATE,
			};
			filters = new String[] {
					ReportsPlugin.getResourceString("Report.AccountBalances.AllEntries"),
					ReportsPlugin.getResourceString("Entry.date"),
			};
		}
	}
		
	ViewerComposite viewer;
	
	private Label filterLabel;
	private Combo filterBox;
	private Label dateLabel;
	private Text dateField;
	private Button generateButton;

	/**
	 * The Shell to use for all message dialogs.
	 */
	Shell shell;
	
	private VerySimpleDateFormat dateFormat =	
		new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());	

	private Date date;

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	private Composite createContent(Session session, Composite parent) {
		/**
		 * topLevelControl is a control with grid layout, 
		 * onto which all sub-controls should be placed.
		 */
		Composite topLevelControl = new Composite(parent, SWT.NULL);
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		topLevelControl.setLayout(layout);
		
		shell = topLevelControl.getShell(); 

		viewer = new ViewerComposite(topLevelControl, SWT.NONE);
		
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		viewer.setLayoutData(gridData);
							
		// Set up the area at the bottom for the edit controls.

		Composite editAreaControl = new Composite(topLevelControl, SWT.NULL);
		
		GridLayout editAreaLayout = new GridLayout();
		editAreaLayout.numColumns = 5;
		editAreaControl.setLayout(editAreaLayout);
		
		// Add the controls to the edit area
		
		filterLabel = new Label(editAreaControl, 0);
		filterBox = new Combo(editAreaControl, 0);
		dateLabel = new Label(editAreaControl, 0);
		dateField = new Text(editAreaControl, 0);
		generateButton = new Button(editAreaControl, 0);

		filterLabel.setText(ReportsPlugin.getResourceString("EntryFilterPanel.filter"));
		for (int i = 0; i < filters.length; i++) {
			filterBox.add(filters[i]);
		}
		filterBox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateFilter();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				updateFilter();
			}
		});

		dateLabel.setText(JMoneyPlugin.getResourceString("Entry.date"));
		dateField.setEnabled(false);
		dateField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateDate();
			}
		});

		generateButton.setText(ReportsPlugin.getResourceString("Panel.Report.Generate"));
		generateButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				generateReport();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				generateReport();
			}
		});

//		generateButton.setEnabled(true);

		return topLevelControl;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
		SectionlessPage formPage = new SectionlessPage(
				editor,
				PAGE_ID, 
				"Account Balances Report", 
				"Account Balances Report") {
			
			public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
				Session session = JMoneyPlugin.getDefault().getSession();
				Composite control = createContent(session, parent);
				
				// If a memento is passed, restore the field contents
				if (memento != null) {
					Integer filterType = memento.getInteger("filter");
					if (filterType != null) {
						int index = 0;
						while (index < filterIndexes.length) {
							if (filterIndexes[index] == filterType.intValue()) {
								break;
							}
							index++;
						}
						
						if (index < filterIndexes.length) {
							filterBox.select(index);
							if (filterType.intValue() == DATE) {
								dateField.setEnabled(true);
								String dateString = memento.getString("date");
								if (dateString != null) {
									dateField.setText(dateString);
								}
							}
						}
					}
				}
				
				return control;
			}

			public void saveState(IMemento memento) {
				int selectionIndex = filterBox.getSelectionIndex();
				if (selectionIndex != -1) {
					memento.putInteger("filter", filterIndexes[selectionIndex]);
					if (filterIndexes[selectionIndex] == DATE) {
						memento.putString("date", dateField.getText());
					}
				}
			}
		};

		try {
			editor.addPage(formPage);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
			// TODO: cleanly leave out this page.
		}
		
		return formPage;
	}

	private void updateFilter() {
		int selectionIndex = filterBox.getSelectionIndex();
		if (selectionIndex != -1) {
			dateField.setEnabled(filterIndexes[selectionIndex] == DATE);
		}
	}

	private void updateDate() {
		date = dateFormat.parse(dateField.getText());
		dateField.setText(dateFormat.format(date));
	}

	private void generateReport() {
		try {
			URL url =
				ReportsPlugin.class.getResource("resources/AccountBalances.jasper");
			if (url == null) {
				System.err.println("resources/AccountBalances.jasper not found.  A manual build of the net.sf.jmoney.reports project may be necessary.");
				JMoneyPlugin.log(new Status(IStatus.ERROR, ReportsPlugin.PLUGIN_ID, IStatus.ERROR, "resources/AccountBalances.jasper not found.  A manual build of the net.sf.jmoney.reports project may be necessary.", null));
	            MessageDialog errorDialog = new MessageDialog(
	                    shell,
	                    "JMoney Build Error",
	                    null, // accept the default window icon
	                    "resources/AccountBalances.jasper not found.  A manual build of the net.sf.jmoney.reports project may be necessary.",
	                    MessageDialog.ERROR,
	                    new String[] { IDialogConstants.OK_LABEL }, 0);
	            errorDialog.open();
				return;
			}
			InputStream is = url.openStream();

			Map params = new HashMap();
			params.put(
				"Title",
				ReportsPlugin.getResourceString("Report.AccountBalances.Title"));
			params.put("Subtitle", getSubtitle());
			params.put("Total", ReportsPlugin.getResourceString("Report.Total"));
			params.put(
				"Account",
				ReportsPlugin.getResourceString("Report.AccountBalances.Account"));
			params.put(
				"Balance",
				ReportsPlugin.getResourceString("Report.AccountBalances.Balance"));
			params.put("DateToday", dateFormat.format(new Date()));
			params.put("Page", ReportsPlugin.getResourceString("Report.Page"));

			Collection items = getItems();
			if (items.isEmpty()) {
				MessageDialog dialog =
					new MessageDialog(
							shell,
							ReportsPlugin.getResourceString("Panel.Report.EmptyReport.Title"), 
							null, // accept the default window icon
							ReportsPlugin.getResourceString("Panel.Report.EmptyReport.Message"), 
							MessageDialog.ERROR, 
							new String[] { IDialogConstants.OK_LABEL }, 0);
				dialog.open();
			} else {
				JRDataSource ds = new JRBeanCollectionDataSource(items);
				JasperPrint print =
					JasperFillManager.fillReport(is, params, ds);
				viewer.getReportViewer().setDocument(print);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private String getSubtitle() {
		int selectedIndex = filterBox.getSelectionIndex();
		if (selectedIndex >= 0) {
			switch (filterIndexes[selectedIndex]) {
				case ALL_ENTRIES :
					return ReportsPlugin.getResourceString("Report.AccountBalances.AllEntries");
				case CLEARED_ENTRIES :
					return ReportsPlugin.getResourceString(
					"Report.AccountBalances.ClearedEntries");
				case DATE :
					return dateFormat.format(date);
			}
		}
		return "";
	}

	private Collection getItems() {
		Vector items = new Vector();

		Session session = JMoneyPlugin.getDefault().getSession();
		Iterator aIt = session.getCapitalAccountIterator();
		while (aIt.hasNext()) {
			CapitalAccount account = (CapitalAccount) aIt.next();
			// TODO: Accounts which contain multiple currencies
			// must also be shown.
			if (account instanceof CurrencyAccount) {
				CurrencyAccount currencyAccount = (CurrencyAccount)account;
				long bal = currencyAccount.getStartBalance();
				
				Iterator eIt = account.getEntries().iterator();
				while (eIt.hasNext()) {
					Entry e = (Entry) eIt.next();
					if (accept(e))
						bal += e.getAmount();
				}
				
				items.add(new Item(currencyAccount, bal));
			}
		}

		Collections.sort(items);
		return items;
	}

	private boolean accept(Entry entry) {
		int selectedIndex = filterBox.getSelectionIndex();
		if (selectedIndex >= 0) {
			switch (filterIndexes[selectedIndex]) {
				case ALL_ENTRIES :
					return true;
				case CLEARED_ENTRIES :
					// This plug-in does not depend on the reconciliation plug-in.
					// Therefore we have no access to classes and constants in the plug-in.
					// This following constant must match that in the plug-in.
					// It should not be possible to get to this case if the
					// property was not found.
					final int CLEARED = 2;
					int status = entry.getIntegerPropertyValue(reconciliationStatusAccessor);
					return status == CLEARED;
				case DATE :
					return acceptTo(entry.getTransaction().getDate());
			}
		}
		return true;
	}

	private boolean acceptTo(Date d) {
		if (date == null)
			return true;
		if (d == null)
			return false;
		return (d.before(date) || d.equals(date));
	}

	public class Item implements Comparable {

		private CurrencyAccount account;

		private long balance;

		public Item(CurrencyAccount anAccount, long aBalance) {
			account = anAccount;
			balance = aBalance;
		}

		public CapitalAccount getAccount() {
			return account;
		}

		public String getAccountName() {
			return account.getName();
		}

		public Long getBalance() {
			return new Long(balance);
		}

		public String getBalanceString() {
			return account.getCurrency().format(balance);
		}

		public void addToBalance(long amount) {
		}

		public String getCurrencyCode() {
			return account.getCurrency().getCode();
		}

		public int compareTo(Object o) {
			return account.compareTo(((Item) o).getAccount());
		}
	}
}
