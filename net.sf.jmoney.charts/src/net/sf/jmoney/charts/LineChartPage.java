
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
public class LineChartPage implements IBookkeepingPage {

    private static final String PAGE_ID = "net.sf.jmoney.charts.lineChart";
    private Session session;
    private Tree tree;
    private Text fromDate, toDate;
    private final DateFormat df;
    private Button chkDaily, chkAverage30, chkAverage120;
    private Button radSaldo, radMouvement;
    
    /**
     * Constructor
     *
     */
    public LineChartPage () {
        super();
		df = new SimpleDateFormat();
    }
    
	public void init(IMemento memento) {
		// No view state to restore
	}

	public void saveState(IMemento memento) {
		// No view state to save
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#getPageCount(java.lang.Object)
	 */
	public int getPageCount(Object selectedObject) {
		return 1;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public Composite createContent(final Session session, Composite parent) {
	    
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
	    addAccountsInTree(treeItem, session.getCapitalAccountIterator());

	    // Add other components
	    Group typeGroup = new Group(swingComposite, SWT.NULL);
	    typeGroup.setText("Type");
	    typeGroup.setLayout(new CellLayout(2));
	    
	    Group actionGroup = new Group(swingComposite, SWT.NULL);
	    actionGroup.setText("Actions");
	    actionGroup.setLayout(new CellLayout(2));
	    
		(new Label(actionGroup, SWT.NULL)).setText("From date:");
		fromDate = new Text (actionGroup, SWT.NULL);
		fromDate.setText(df.format(new Date(0)));
		
		(new Label(actionGroup, SWT.NULL)).setText("To date:");
		toDate = new Text (actionGroup, SWT.NULL);
		toDate.setText(df.format(new Date()));

		chkDaily = new Button(actionGroup, SWT.CHECK);
		chkDaily.setText("Daily");
		
		chkAverage30 = new Button(actionGroup, SWT.CHECK);
		chkAverage30.setText("Average 30 days");

		chkAverage120 = new Button(actionGroup, SWT.CHECK);
		chkAverage120.setText("Average 120 days");

		radSaldo = new Button(typeGroup, SWT.RADIO);
		radSaldo.setText("Saldo");
		
		radMouvement = new Button(typeGroup, SWT.RADIO);
		radMouvement.setText("Mouvement");
	
		// Add the "Draw" Button
		
		Button redraw = new Button(actionGroup, SWT.NULL);
		redraw.setText("Draw!");

		redraw.addSelectionListener(new SelectionAdapter () {
		    public void widgetSelected (SelectionEvent e) { createChart(); }
		});


		
	    return swingComposite;
}

/**
 * Add each element (account) of the Iterator in the tree and 
 * add all its subaccounts too as subtree  
 * @param tn TreeItem the accounts are to add to.
 * @param it accounts to add
 */
private void addAccountsInTree(TreeItem tn, Iterator it) {
	while (it.hasNext()) {
	    CapitalAccount a2 = (CapitalAccount) it.next();
	    TreeItem treeItem = new TreeItem(tn, SWT.NULL);
	    treeItem.setText(a2.getName());
	    treeItem.setData(a2);
	    if (util.getEntriesFromAccount(session, a2).size() == 0) {
	        treeItem.setText(a2.getName() + " (no entries)");
	        treeItem.setGrayed(true);
	    }
	    addAccountsInTree(treeItem, a2.getSubAccountIterator());
	}
}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IFormPage createFormPage(NodeEditor editor) {
		return new SectionlessPage(
				editor,
				PAGE_ID, 
				"Chart", 
				"Line Chart") {
			
			public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit) {
				Session session = JMoneyPlugin.getDefault().getSession();
				return createContent(session, parent);
			}
		};
	}

/**
 * 
 */
private void createChart() {
    final LineChart chart;
    // Prepare the parameters
    LineChartParameters params = new LineChartParameters();

    Vector accounts = new Vector();
    addChosenAccountsToVector(tree.getItems()[0],accounts);
    params.setAccountList(accounts);

    params.setDates(fromDate.getText(),toDate.getText()); 
    params.setDaily(chkDaily.getSelection());
    params.setAverage30(chkAverage30.getSelection());
    params.setAverage120(chkAverage120.getSelection());
    
    if (radMouvement.getSelection()) 	params.setType(LineChartParameters.MOUVEMENT);
    if (radSaldo.getSelection()) 		params.setType(LineChartParameters.SALDO);
    
    
    chart = new  ActivLineChart("myChart", session, params);
    final ChartPanel chartPanel = chart.getChartPanel();
    chartPanel.setPreferredSize(new Dimension(500,270));
    chart.createOrUpdateValues(params);
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

