
package net.sf.jmoney.charts;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.Date;
import java.util.Vector;
import java.awt.BorderLayout;
import java.awt.Dimension;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ColumnLayout;
import org.eclipse.ui.internal.dnd.SwtUtil;
import org.eclipse.ui.internal.layout.CellLayout;
import org.eclipse.ui.internal.layout.LayoutUtil;
import org.eclipse.ui.internal.layout.TrimLayout;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.data.time.TimeSeries;
import org.jfree.xml.generator.SplittingModelWriter;
import org.w3c.dom.events.MouseEvent;

import sun.awt.AWTAutoShutdown;

import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

/**
 * @author Faucheux
 */
public class StackedChartPage implements IBookkeepingPageFactory {

    private static final String PAGE_ID = "net.sf.jmoney.charts.lineChart";
    private Session session;
    private Tree tree;
    private Text fromDate, toDate, maxLevel;
    private final DateFormat df;
    private Button radPeriodDay, radPeriodMonth, radPeriodYear;
    
    /**
     * Constructor
     *
     */
    public StackedChartPage () {
        super();
		df = new SimpleDateFormat();
    }
    
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public Composite createContent(final Session session, Composite parent, Vector selectedAccounts, Vector selectedTreeItems) {
	    
	    this.session = session;
	    Composite swingComposite = new Composite(parent, SWT.EMBEDDED);
	    swingComposite.setLayout(new FillLayout());

	    // Add Account Tree
	    Group accountGroup = new Group(swingComposite, SWT.NULL);
	    accountGroup.setText("Accounts");
	    accountGroup.setLayout(new FillLayout());

	    tree = new Tree(accountGroup, SWT.CHECK | SWT.MULTI | SWT.VERTICAL);
	    TreeItem treeItem = new TreeItem(tree, SWT.NULL);
	    treeItem.setText("Accounts");  // TODO - Faucheux Internationlization
	    addAccountsInTree(treeItem, session.getCapitalAccountIterator(), selectedAccounts, selectedTreeItems);

	    // Add other components (Parameters)
	    
	    Composite parameterContainer = new  Composite(swingComposite, SWT.EMBEDDED);
	    parameterContainer.setLayout(new FillLayout(SWT.VERTICAL));
	    
	    Group groupPeriod = new Group(parameterContainer, SWT.NULL);
	    groupPeriod.setText("Period");
	    groupPeriod.setLayout(new CellLayout(2));
	    
	    Group actionGroup = new Group(parameterContainer, SWT.NULL);
	    actionGroup.setText("Actions");
	    actionGroup.setLayout(new CellLayout(2));
	    
		(new Label(actionGroup, SWT.NULL)).setText("From date:");
		fromDate = new Text (actionGroup, SWT.NULL);
		fromDate.setText(df.format(new Date(0)));
		
		(new Label(actionGroup, SWT.NULL)).setText("To date:");
		toDate = new Text (actionGroup, SWT.NULL);
		toDate.setText(df.format(new Date()));

		(new Label(actionGroup, SWT.NULL)).setText("Maximal level:");
		maxLevel = new Text (actionGroup, SWT.NULL);
		maxLevel.setText("2");

		radPeriodDay = new Button(groupPeriod, SWT.RADIO);
		radPeriodDay.setText("Day");
		
		radPeriodMonth = new Button(groupPeriod, SWT.RADIO);
		radPeriodMonth.setText("Month");

		radPeriodYear = new Button(groupPeriod, SWT.RADIO);
		radPeriodYear.setText("Years");
	
		// Add the "Draw" Button
		
		Button drawButton = new Button(actionGroup, SWT.NULL);
		drawButton.setText("Draw!");

		drawButton.addSelectionListener(new SelectionAdapter () {
		    public void widgetSelected (SelectionEvent e) { createChart(); }
		});


		
	    return swingComposite;
}

/**
 * Add each element (account) of the Iterator in the tree and 
 * add all its subaccounts too as subtree
 * <P>
 * This method also checks if the account is in the list of
 * selected accounts and, if so, adds the TreeItem to the list of
 * selected tree items.  This must be done so that the set of
 * selected accounts are persisted when the workbench is closed and
 * re-opened.
 *   
 * @param tn TreeItem the accounts are to add to.
 * @param it accounts to add
 */
	private void addAccountsInTree(TreeItem tn, Iterator it, Vector selectedAccounts, Vector selectedTreeItems) {
	while (it.hasNext()) {
	    CapitalAccount a2 = (CapitalAccount) it.next();
	    TreeItem treeItem = new TreeItem(tn, SWT.NULL);
	    treeItem.setText(a2.getName());
	    treeItem.setData(a2);
	    if (Util.getEntriesFromAccount(session, a2).size() == 0) {
	        treeItem.setText(a2.getName() + " (no entries)");
	        treeItem.setGrayed(true);
	    }
	    
	    // If the account is in the list of selected accounts
	    // then add the tree item to the list of selected tree items.
	    if (selectedAccounts.indexOf(a2) != -1) {
	    	selectedTreeItems.add(treeItem);
	    }
	    
	    addAccountsInTree(treeItem, a2.getSubAccountIterator(), selectedAccounts, selectedTreeItems);
	}
}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
		SectionlessPage formPage = new SectionlessPage(
				editor,
				PAGE_ID, 
				"Chart", 
				"Line Chart") {
			
			public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
				Session session = JMoneyPlugin.getDefault().getSession();

				Vector selectedAccounts = new Vector();
				Vector selectedTreeItems = new Vector();
				if (memento != null) {
					IMemento[] accountMementos = memento.getChildren("selectedAccount");
					for (int i = 0; i < accountMementos.length; i++) {
						String fullAccountName = accountMementos[i].getString("name");
						if (fullAccountName != null) {
							Account account = session.getAccountByFullName(fullAccountName);
							if (account != null) {
								selectedAccounts.add(account);
							}
						}
					}
				}
				
				Composite control = createContent(session, parent, selectedAccounts, selectedTreeItems);
				
				// Check the selected accounts
				TreeItem[] array = (TreeItem[])selectedTreeItems.toArray(new TreeItem[selectedTreeItems.size()]);
				for (int j=0; j<array.length; j++) {
					System.out.println(array[j].toString());
					array[j].setChecked(true);
				}
				// Also select them.  This causes the tree to be expanded so that
				// the user can see the selection.
				tree.setSelection(array);
				
				return control;
			}

			public void saveState(IMemento memento) {
				// Save the selected accounts
			    Vector accounts = new Vector();
			    addChosenAccountsToVector(tree.getItems()[0],accounts);
			    for (Iterator iter = accounts.iterator(); iter.hasNext(); ) {
			    	String selectedAccountName = (String)iter.next();
			    	memento.createChild("selectedAccount").putString("name", selectedAccountName);
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

/**
 * 
 */
private void createChart() {
    final StackedChart chart;
    // Prepare the parameters
    
    StackedChartParameters params = new StackedChartParameters();

    Vector accounts = new Vector();
    addChosenAccountsToVector(tree.getItems()[0],accounts);
    params.setAccountList(accounts);

    params.setDates(fromDate.getText(),toDate.getText());
    System.out.println("maxLevel: " + maxLevel.getText() );
    params.setMaxLevel(Integer.parseInt(maxLevel.getText()));
    
    if (radPeriodDay.getSelection())   params.setFrequence(StackedChartParameters.DAY);
    if (radPeriodMonth.getSelection()) params.setFrequence(StackedChartParameters.MONTH);
    if (radPeriodYear.getSelection())  params.setFrequence(StackedChartParameters.YEAR);
    
    chart = new StackedAccountChart("Chart", session, params);
    final ChartPanel chartPanel = chart.getChartPanel();
    chartPanel.setPreferredSize(new Dimension(500,270));
    // chart.createOrUpdateValues(params);
    chart.displayAsWindow();
}

/**
 * 
 */
private void addChosenAccountsToVector(TreeItem treeitem, Vector vector) {
    for (int i = 0; i < treeitem.getItemCount(); i++) {
        if (treeitem.getItems()[i].getChecked()) {
            vector.add(((Account) treeitem.getItems()[i].getData()).getFullAccountName());
        }
        addChosenAccountsToVector(treeitem.getItems()[i],vector);
	}
}
	

}

