
package net.sf.jmoney.charts;


import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.Date;
import java.util.Vector;
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
import org.eclipse.ui.internal.dnd.SwtUtil;
import org.eclipse.ui.internal.layout.CellLayout;
import org.eclipse.ui.internal.layout.LayoutUtil;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.data.time.TimeSeries;
import org.jfree.xml.generator.SplittingModelWriter;
import org.w3c.dom.events.MouseEvent;

import sun.awt.AWTAutoShutdown;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Account;
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
	    Group actionGroup = new Group(swingComposite, SWT.NULL);
	    actionGroup.setText("Actions");
	    actionGroup.setLayout(new CellLayout(2));
	    
		DateFormat df = new SimpleDateFormat();
		
		(new Label(actionGroup, SWT.NULL)).setText("From date:");
		Text fromDate = new Text (actionGroup, SWT.NULL);
		fromDate.setText(df.format(new Date(0)));
		
		(new Label(actionGroup, SWT.NULL)).setText("To date:");
		Text toDate = new Text (actionGroup, SWT.NULL);
		toDate.setText(df.format(new Date()));

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
	    Account a2 = (Account) it.next();
	    TreeItem treeItem = new TreeItem(tn, SWT.NULL);
	    treeItem.setText(a2.getName());
	    treeItem.setData(a2);
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
			
			public Composite createControl(Object nodeObject, Composite parent) {
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
    chart = new  ActivLineChart("myChart", session);
    final ChartPanel chartPanel = chart.getChartPanel();
    chartPanel.setPreferredSize(new Dimension(500,270));
    
    
    // Give the accounts for the chart
    Vector accounts = new Vector();
    addChosenAccountsToVector(tree.getItems()[0],accounts); 

    // chart.setDates((Date) fromDate.getText(), (Date) toDate.getValue());
    chart.createOrUpdateValues(accounts);
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

