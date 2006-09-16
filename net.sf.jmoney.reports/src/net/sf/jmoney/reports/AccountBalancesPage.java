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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jasperreports.engine.JRAlignment;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExpression;
import net.sf.jasperreports.engine.JRVariable;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignReportFont;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JRDesignVariable;
import net.sf.jasperreports.engine.design.JRVerifier;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.fill.JRCalculator;
import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.fields.DateControl;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.CurrencyAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyNotFoundException;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

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
public class AccountBalancesPage implements IBookkeepingPageFactory {
	
	private static final String PAGE_ID = "net.sf.jmoney.reports.accountBalances";
	
	public static final int ALL_ENTRIES = 0;
	
	public static final int CLEARED_ENTRIES = 1;
	
	public static final int DATE = 2;
	
	public static int[] filterIndexes;
	
	public static String[] filters;
	
	public static ScalarPropertyAccessor<?> reconciliationStatusAccessor;
	
	static {
		try {
			reconciliationStatusAccessor = (ScalarPropertyAccessor)PropertySet.getPropertyAccessor("net.sf.jmoney.reconciliation.entryProperties.status");
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
	private DateControl dateField;
	private Button generateButton;
	
	/**
	 * The Shell to use for all message dialogs.
	 */
	Shell shell;
	
	private Date date;
	
	protected Map expressionMap;
	
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
		dateField = new DateControl(editAreaControl);
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
									try {
										VerySimpleDateFormat dateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
										Date date = dateFormat.parse(dateString);
										dateField.setDate(date);
									} catch (IllegalArgumentException e) {
										// Ignore and leave date blank if the date from the
										// memento does not parse
									}
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
						VerySimpleDateFormat dateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
						memento.putString("date", dateFormat.format(dateField.getDate()));
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
		date = dateField.getDate();
		dateField.setDate(date);
	}
	
	private void generateReport() {
		try {
			Map params = new HashMap();
			/* Parameters are no longer needed because the Java
			 * code can simply obtain the values while generating
			 * the report.			
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
			 */
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
				
				expressionMap = new HashMap();
				
				JasperDesign jasperDesign = buildDesign();
				JRCalculator jasperCalculator = new AccountBalancesCalculator(expressionMap); 
				JasperReport jasperReport = compileReport(jasperDesign, jasperCalculator);
				JasperPrint print =
					JasperFillManager.fillReport(jasperReport, params, ds);
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
				VerySimpleDateFormat dateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
				return dateFormat.format(date);
			}
		}
		return "";
	}
	
	private JasperDesign buildDesign() {
		VerySimpleDateFormat dateFormat = new VerySimpleDateFormat(JMoneyPlugin.getDefault().getDateFormat());
		String dateToday = dateFormat.format(new Date());
		
		try {
			JasperDesign jasperDesign = new JasperDesign();
			
			jasperDesign.setName("AccountBalances");
			jasperDesign.setPageWidth(595);
			jasperDesign.setPageHeight(842);
			jasperDesign.setColumnWidth(515);
			jasperDesign.setColumnSpacing(0);
			jasperDesign.setLeftMargin(40);
			jasperDesign.setRightMargin(40);
			jasperDesign.setTopMargin(50);
			jasperDesign.setBottomMargin(50);
			
			JRDesignReportFont reportFont1 = new JRDesignReportFont();
			reportFont1.setName("plain");
			reportFont1.setDefault(true);
			jasperDesign.addFont(reportFont1);
			
			JRDesignReportFont reportFont2 = new JRDesignReportFont();
			reportFont2.setName("bold");
			reportFont2.setBold(true);
			reportFont2.setPdfFontName("Helvetica-Bold");
			jasperDesign.addFont(reportFont2);
			
			JRDesignReportFont reportFont3 = new JRDesignReportFont();
			reportFont3.setName("italic");
			reportFont3.setItalic(true);
			reportFont3.setPdfFontName("Helvetica-Oblique");
			jasperDesign.addFont(reportFont3);
			
			// Fields
			jasperDesign.addField(createField("accountName", String.class));
			jasperDesign.addField(createField("balance", Long.class));
			jasperDesign.addField(createField("balanceString", String.class));
			jasperDesign.addField(createField("currencyCode", String.class));
			
			// Groups
			JRDesignGroup currencyGroup = new JRDesignGroup();
			currencyGroup.setName("CurrencyGroup");
			currencyGroup.setMinHeightToStartNewPage(60);
			currencyGroup.setExpression(createExpression(String.class, AccountBalancesCalculator.CURRENCY_CODE));
			
			// Group header
			{
				JRDesignBand band = new JRDesignBand();
				band.setHeight(0);
				currencyGroup.setGroupHeader(band);
			}
			
			// Group footer
			{
				JRDesignBand band = new JRDesignBand();
				band.setHeight(35);
				{
					JRDesignLine line = new JRDesignLine();
					line.setX(0);
					line.setY(0);
					line.setWidth(515);
					line.setHeight(0);
					band.addElement(line);
				}
				{
					JRDesignTextField textField = new JRDesignTextField();
					textField.setX(0);
					textField.setY(5);
					textField.setWidth(415);
					textField.setHeight(13);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setBold(true);
						textField.setFont(reportFont);
					}
					textField.setExpression(createExpression(String.class, AccountBalancesCalculator.TOTAL_FOR_CURRENCY_TEXT));
					band.addElement(textField);
				}
				{
					JRDesignTextField textField = new JRDesignTextField();
					textField.setX(420);
					textField.setY(5);
					textField.setWidth(95);
					textField.setHeight(13);
					textField.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_RIGHT);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setBold(true);
						textField.setFont(reportFont);
					}
					textField.setExpression(createExpression(String.class, AccountBalancesCalculator.TOTAL_FOR_CURRENCY));
					band.addElement(textField);
				}
				{
					JRDesignLine line = new JRDesignLine();
					line.setX(0);
					line.setY(22);
					line.setWidth(515);
					line.setHeight(0);
					band.addElement(line);
				}
				currencyGroup.setGroupFooter(band);
			}
			jasperDesign.addGroup(currencyGroup);	
			
			// Variables used to accumulate data for the group footer
			{
				JRDesignVariable variable = new JRDesignVariable();
				variable.setName("BalanceSum");
				variable.setValueClassName("java.lang.Long");
				variable.setResetType(JRVariable.RESET_TYPE_GROUP);
				variable.setResetGroup(currencyGroup);
				variable.setCalculation(JRVariable.CALCULATION_SUM);
				variable.setExpression(createExpression(Long.class, AccountBalancesCalculator.BALANCE_FIELD));
				variable.setInitialValueExpression(createExpression(Long.class, AccountBalancesCalculator.ZERO));
				jasperDesign.addVariable(variable);
			}
			
			// Title
			{
				JRDesignBand band = new JRDesignBand();
				band.setHeight(50);
				{
					JRDesignStaticText textField = new JRDesignStaticText();
					textField.setX(0);
					textField.setY(0);
					textField.setWidth(515);
					textField.setHeight(16);
					textField.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_CENTER);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setBold(true);
						reportFont.setSize(12);
						textField.setFont(reportFont);
					}
					textField.setText(ReportsPlugin.getResourceString("Report.AccountBalances.Title"));
					band.addElement(textField);
				}
				{
					JRDesignStaticText textField = new JRDesignStaticText();
					textField.setX(0);
					textField.setY(17);
					textField.setWidth(515);
					textField.setHeight(13);
					textField.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_CENTER);
					textField.setText(getSubtitle());
					band.addElement(textField);
				}
				jasperDesign.setTitle(band);
			}
			
			// Page Header
			{
				JRDesignBand band = new JRDesignBand();
				band.setHeight(17);
				{
					JRDesignStaticText textField = new JRDesignStaticText();
					textField.setX(0);
					textField.setY(0);
					textField.setWidth(415);
					textField.setHeight(13);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setBold(true);
						textField.setFont(reportFont);
					}
					textField.setText(ReportsPlugin.getResourceString("Report.AccountBalances.Account"));
					band.addElement(textField);
				}
				{
					JRDesignStaticText textField = new JRDesignStaticText();
					textField.setX(420);
					textField.setY(0);
					textField.setWidth(95);
					textField.setHeight(13);
					textField.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_RIGHT);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setBold(true);
						textField.setFont(reportFont);
					}
					textField.setText(ReportsPlugin.getResourceString("Report.AccountBalances.Balance"));
					band.addElement(textField);
				}
				{
					JRDesignLine line = new JRDesignLine();
					line.setX(0);
					line.setY(12);
					line.setWidth(515);
					line.setHeight(0);
					band.addElement(line);
				}
				jasperDesign.setPageHeader(band);
			}
			
			// Detail
			{
				JRDesignBand band = new JRDesignBand();
				band.setHeight(17);
				{
					JRDesignTextField textField = new JRDesignTextField();
					textField.setX(0);
					textField.setY(0);
					textField.setWidth(415);
					textField.setHeight(13);
					textField.setExpression(createExpression(String.class, AccountBalancesCalculator.ACCOUNT_NAME_FIELD));
					band.addElement(textField);
				}
				{
					JRDesignTextField textField = new JRDesignTextField();
					textField.setX(420);
					textField.setY(0);
					textField.setWidth(95);
					textField.setHeight(13);
					textField.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_RIGHT);
					textField.setExpression(createExpression(String.class, AccountBalancesCalculator.BALANCE_STRING_FIELD));
					band.addElement(textField);
				}
				jasperDesign.setDetail(band);
			}
			
			// Page Footer
			{
				JRDesignBand band = new JRDesignBand();
				band.setHeight(30);
				{
					JRDesignStaticText textField = new JRDesignStaticText();
					textField.setX(0);
					textField.setY(17);
					textField.setWidth(200);
					textField.setHeight(13);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setSize(10);
						textField.setFont(reportFont);
					}
					textField.setText(dateToday);
					band.addElement(textField);
				}
				{
					JRDesignTextField textField = new JRDesignTextField();
					textField.setX(315);
					textField.setY(17);
					textField.setWidth(200);
					textField.setHeight(13);
					textField.setHorizontalAlignment(JRAlignment.HORIZONTAL_ALIGN_RIGHT);
					{
						JRDesignReportFont reportFont = new JRDesignReportFont();
						reportFont.setSize(10);
						textField.setFont(reportFont);
					}
					textField.setExpression(createExpression(String.class, AccountBalancesCalculator.PAGE_NUMBER));
					band.addElement(textField);
				}
				jasperDesign.setPageFooter(band);
			}
			
			return jasperDesign;
		} catch (JRException e) {
			// This exception should not happen unless there is a
			// bug in the above code.
			throw new RuntimeException("internal error - jasper report design error");
		}
	}
	
	private JRDesignField createField(String name, Class classOfField) {
		JRDesignField field = new JRDesignField();
		field.setName(name);
		field.setValueClassName(classOfField.getName());
		return field;
	}
	
	private JRExpression createExpression(Class expressionClass, int expressionNumber) {
		JRDesignExpression expression = new JRDesignExpression();
		expression.setValueClassName(expressionClass.getName());
		expressionMap.put(new Integer(expression.getId()), new Integer(expressionNumber));
		return expression;
	}
	
	public JasperReport compileReport(JasperDesign jasperDesign, JRCalculator calculator) {
		Collection brokenRules = JRVerifier.verifyDesign(jasperDesign);
		if (brokenRules != null && brokenRules.size() > 0)
		{
			StringBuffer sbuffer = new StringBuffer();
			sbuffer.append("Report design not valid : ");
			int i = 1;
			for(Iterator it = brokenRules.iterator(); it.hasNext(); i++)
			{
				sbuffer.append("\n\t " + i + ". " + (String)it.next());
			}
			throw new RuntimeException(sbuffer.toString());
		}
		
		//Report design OK
		
		/*
		 * We use a little trick here.  The JasperReport constructor
		 * takes as a parameter the name of a class that implements
		 * the JRCompiler interface and whose loadCalculator method
		 * must return a JRCalculator object suitable for the report.
		 * 
		 * We already have the calculator object.  It does not have
		 * to be compiled or anything complicated as JasperReports
		 * assumes.  We cannot pass this object thru as a parameter
		 * so we instead set it into a static field.  As reports all
		 * run on the same thread, this mechanism is ok.
		 */
		MyDummyCompiler.calculator = calculator; 
		JasperReport jasperReport = 
			new JasperReport(
					jasperDesign,
					MyDummyCompiler.class.getName(),
					null
			);
		
		return jasperReport;
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
				int status = (Integer)entry.getPropertyValue(reconciliationStatusAccessor);
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
		
		public String getCurrencyCode() {
			return account.getCurrency().getCode();
		}
		
		public int compareTo(Object o) {
			return account.compareTo(((Item) o).getAccount());
		}
	}
}
