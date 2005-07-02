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
import java.util.Calendar;
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
import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.IncomeExpenseAccount;
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
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.jasperassistant.designer.viewer.ViewerComposite;

/**
 * @author Nigel Westbury
 */
public class IncomeExpensePage implements IBookkeepingPageFactory {

    private static final String PAGE_ID = "net.sf.jmoney.reports.incomeAndExpense";
    
	public static final int THIS_MONTH = 0;

	public static final int THIS_YEAR = 1;

	public static final int LAST_MONTH = 2;

	public static final int LAST_YEAR = 3;

	public static final int CUSTOM = 4;

	public static final String[] periods =
		{
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.thisMonth"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.thisYear"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.lastMonth"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.lastYear"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.custom")
		};

	private ViewerComposite viewer;

	private Label periodLabel;
	private Combo periodBox;
	private Label fromLabel;
	private DateControl fromField;
	private Label toLabel;
	private DateControl toField;
	private Button subtotalsCheckBox;
	private Button generateButton;

	/**
	 * The Shell to use for all message dialogs.
	 */
	Shell shell;
	
	private VerySimpleDateFormat dateFormat =	
		new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());	

	private Date fromDate;
	private Date toDate;

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
		editAreaLayout.numColumns = 7;
		editAreaControl.setLayout(editAreaLayout);
		
		// Add the controls to the edit area
		
		periodLabel = new Label(editAreaControl, 0);
		periodBox = new Combo(editAreaControl, 0);
		fromLabel = new Label(editAreaControl, 0);
		fromField = new DateControl(editAreaControl);
		toLabel = new Label(editAreaControl, 0);
		toField = new DateControl(editAreaControl);
		generateButton = new Button(editAreaControl, 0);
		subtotalsCheckBox = new Button(editAreaControl, SWT.CHECK);
			
		periodLabel.setText(
				ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.Period"));
		for (int i = 0; i < periods.length; i++) {
			periodBox.add(periods[i]);
		}
		periodBox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateFromAndTo();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				updateFromAndTo();
			}
		});
		fromLabel.setText(
				ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.From"));
		fromField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateFrom();
			}
		});
		toLabel.setText(ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.To"));
		toField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateTo();
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
		
		subtotalsCheckBox.setText(
				ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.ShowSubtotals"));
		
		return topLevelControl;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
		SectionlessPage formPage = new SectionlessPage(
				editor,
				PAGE_ID, 
				"Income & Expense Report", 
				"Income & Expense Report") {
			
			public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
				Session session = JMoneyPlugin.getDefault().getSession();
				Composite control = createContent(session, parent);

				// If a memento is passed, restore the field contents
				if (memento != null) {
					Integer periodType = memento.getInteger("period");
					if (periodType != null) {
						int periodIndex = periodType.intValue();
						periodBox.select(periodIndex);
						if (periodIndex == CUSTOM) {
							String fromDateString = memento.getString("fromDate");
							if (fromDateString != null)	{
								fromDate = dateFormat.parse(fromDateString);
							}
							String toDateString = memento.getString("toDate");
							if (toDateString != null)	{
								toDate = dateFormat.parse(toDateString);
							}
						}
						
						updateFromAndTo();
					}
					
					boolean subtotals = new Boolean(memento.getString("subtotals")).booleanValue();
				}
				
				return control;
			}

			public void saveState(IMemento memento) {
				int period = periodBox.getSelectionIndex();
				if (period != -1) {
					memento.putInteger("period", period);
					if (period == CUSTOM) {
						memento.putString("fromDate", dateFormat.format(fromField.getDate()));
						memento.putString("toDate", dateFormat.format(toField.getDate()));
					}
				}
				memento.putString(
						"subtotals",
						String.valueOf(subtotalsCheckBox.getSelection()));
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

	private void updateFromAndTo() {
		int index = periodBox.getSelectionIndex();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		switch (index) {
			case THIS_MONTH :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				fromDate = cal.getTime();

				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case THIS_YEAR :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				fromDate = cal.getTime();

				cal.add(Calendar.YEAR, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case LAST_MONTH :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.add(Calendar.MONTH, -1);
				fromDate = cal.getTime();

				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case LAST_YEAR :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				cal.add(Calendar.YEAR, -1);
				fromDate = cal.getTime();

				cal.add(Calendar.YEAR, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case CUSTOM :
			default :
				}

		fromField.setDate(fromDate);
		fromField.setEnabled(index == CUSTOM);
		toField.setDate(toDate);
		toField.setEnabled(index == CUSTOM);
	}

	private void updateFrom() {
		fromDate = fromField.getDate();
		fromField.setDate(fromDate);
	}

	private void updateTo() {
		toDate = toField.getDate();
		toField.setDate(toDate);
	}

	private void generateReport() {
		try {
			String reportFile =
				subtotalsCheckBox.getSelection()
					? "resources/IncomeExpenseSubtotals.jasper"
					: "resources/IncomeExpense.jasper";
			URL url = ReportsPlugin.class.getResource(reportFile);
			if (url == null) {
				Object [] messageArgs = new Object[] {
						reportFile
				};
				
				String errorText = new java.text.MessageFormat(
						"{0} not found.  A manual build of the net.sf.jmoney.reports project may be necessary.", 
								java.util.Locale.US)
								.format(messageArgs);
				
				JMoneyPlugin.log(new Status(IStatus.ERROR, ReportsPlugin.PLUGIN_ID, IStatus.ERROR, errorText, null));
	            MessageDialog errorDialog = new MessageDialog(
	                    shell,
	                    "JMoney Build Error",
	                    null, // accept the default window icon
	                    errorText,
	                    MessageDialog.ERROR,
	                    new String[] { IDialogConstants.OK_LABEL }, 0);
	            errorDialog.open();
				return;
			}
			InputStream is = url.openStream();

			Map params = new HashMap();
			params.put(
				"Title",
				ReportsPlugin.getResourceString("Report.IncomeExpense.Title"));
			params.put(
				"Subtitle",
				dateFormat.format(fromDate)
					+ " - "
					+ dateFormat.format(toDate));
			params.put("Total", ReportsPlugin.getResourceString("Report.Total"));
			params.put("Category", ReportsPlugin.getResourceString("Entry.category"));
			params.put(
				"Income",
				ReportsPlugin.getResourceString("Report.IncomeExpense.Income"));
			params.put(
				"Expense",
				ReportsPlugin.getResourceString("Report.IncomeExpense.Expense"));
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

	private Collection getItems() {
		Vector allItems = new Vector();
		
		/**
		 * Map Commodity to HashMap,
		 * where each HashMap maps IncomeExpenseAccount to Item 
		 */
		HashMap byCurrency = new HashMap();

		Session session = JMoneyPlugin.getDefault().getSession();
		Iterator aIt = session.getIncomeExpenseAccountIterator();
		while (aIt.hasNext()) {
			IncomeExpenseAccount a = (IncomeExpenseAccount) aIt.next();
			for (Iterator eIt = a.getEntries().iterator(); eIt.hasNext(); ) {
				Entry e = (Entry) eIt.next();
				if (accept(e)) {
					HashMap items = (HashMap) byCurrency.get(e.getCommodity());
					if (items == null) {
						items = new HashMap();
						byCurrency.put(e.getCommodity(), items);
					}
					
					Item i = (Item) items.get(a);
					if (i == null) {
						i = new Item(a, e.getCommodity(), e.getAmount());
						items.put(e.getAccount(), i);
						allItems.add(i);
					} else {
						i.addToSum(e.getAmount());
					}
				}
			}
		}

		Collections.sort(allItems);
		return allItems;
	}

	private boolean accept(Entry e) {
		return acceptFrom(e.getTransaction().getDate()) && acceptTo(e.getTransaction().getDate());
	}

	private boolean acceptFrom(Date d) {
		if (fromDate == null)
			return false;
		if (d == null)
			return true;
		return (d.after(fromDate) || d.equals(fromDate));
	}

	private boolean acceptTo(Date d) {
		if (toDate == null)
			return true;
		if (d == null)
			return false;
		return (d.before(toDate) || d.equals(toDate));
	}

	public class Item implements Comparable {

		private Account category;
		private Commodity commodity;
		private long sum;

		public Item(Account aCategory, Commodity commodity, long aSum) {
			category = aCategory;
			this.commodity = commodity;
			sum = aSum;
		}

		/**
		 * Get the string that is used to specify the currency in
		 * the report.
		 */
		// TODO: rename this method
		public String getCurrencyCode() {
			
			return ((Currency)commodity).getCode();
		}

		public String getBaseCategory() {
			if (category == null)
				return ReportsPlugin.getResourceString("Report.IncomeExpense.NoCategory");
                        Account baseCategory = category;
                        while (baseCategory.getParent() != null) {
                            baseCategory = baseCategory.getParent();
                        }
			return baseCategory.getName();
		}

		public String getCategory() {
			return category == null
				? ReportsPlugin.getResourceString("Report.IncomeExpense.NoCategory")
				: category.getFullAccountName();
		}

		public Long getIncome() {
			return sum >= 0 ? new Long(sum) : null;
		}

		public String getIncomeString() {
                    return formatAmount(commodity, getIncome());
		}

		public Long getExpense() {
			return sum < 0 ? new Long(-sum) : null;
		}

		public String getExpenseString() {
			return formatAmount(commodity, getExpense());
		}
		
		private String formatAmount(Commodity commodity, Long amount) {
			if (amount == null) {
				return "";
			} else {
				return commodity.format(amount.longValue());
			}
		}
		
		public void addToSum(long amount) {
			sum += amount;
		}

		public boolean noCategory() {
			return category == null;
		}

		public int compareTo(Object o) {
			Item other = (Item) o;
			if (noCategory() && other.noCategory())
				return 0;
			else if (noCategory())
				return 1;
			else if (other.noCategory())
				return -1;
			else
				return getCategory().compareTo(other.getCategory());
		}
	}
}
