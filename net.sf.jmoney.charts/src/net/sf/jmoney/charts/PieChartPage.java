
package net.sf.jmoney.charts;


import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseMotionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.ui.IMemento;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.event.ChartChangeEvent;
import org.w3c.dom.events.MouseEvent;

import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Session;

/**
 * @author Faucheux
 *
 * As each folder view will load its own instances of the extension classes,
 * and each folder view will only display the tab items for a single
 * object at any point of time, this class can cache the tab items
 * and re-use them for each selected object.
 */
public class PieChartPage implements IBookkeepingPageListener {

    protected ExpensePieChart chart;
    protected JSpinner fromDate;
    protected JSpinner toDate;
    protected JTextField maxLevel;
    
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
		return 2;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public BookkeepingPage[] createPages(Object selectedObject, Session session, Composite parent) {

	    
	    
	    // The chart
	    Composite swingComposite = new Composite(parent, SWT.EMBEDDED);
	    final java.awt.Panel pagePanel;
	    {
	        // Create the pagePanel and register it as display of the plugin
			java.awt.Frame accountEntriesFrame = SWT_AWT.new_Frame(swingComposite);
			pagePanel = new java.awt.Panel(new BorderLayout());
			pagePanel.setBackground(Color.pink);
			accountEntriesFrame.add(pagePanel);
			
			// Create the optionsPanel and add it to pagePanel
			Panel optionsPanel = new Panel(new FlowLayout());
			pagePanel.add(optionsPanel, BorderLayout.NORTH);
			optionsPanel.setBackground(Color.magenta); 
			
			// Create the graphPanel and add it to pagePanel
			GridBagLayout gbLayout = new GridBagLayout();
			Panel graphPanel = new Panel (gbLayout);
			pagePanel.add(graphPanel, BorderLayout.CENTER);
			GridBagConstraints gbConstraints = new GridBagConstraints();
			graphPanel.setBackground(Color.GREEN);
			

			// Create the (main) graph and add it to graphPanel
			chart = new  ExpensePieChart("myChart", session, 1);
			chart.run();
			ChartPanel chartPanel = chart.getChartPanel();
			gbConstraints.fill = GridBagConstraints.BOTH;
			gbConstraints.anchor = GridBagConstraints.NORTHWEST;
			gbConstraints.weightx = 1; gbConstraints.weighty = 1;
			gbConstraints.gridwidth = 3;
			gbLayout.setConstraints(chartPanel, gbConstraints);
			graphPanel.add(chartPanel);
			chartPanel.addChartMouseListener(chart);
			chart.setContainerForSeveralGraphics(graphPanel);
			

			// final TextField fromDate = new TextField("from");    optionsPanel.add(fromDate);
			fromDate = new JSpinner(new SpinnerDateModel(new Date(0), null, null, Calendar.DATE));
			optionsPanel.add(fromDate);
			fromDate.addChangeListener( new redrawGraph());
			
			toDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MONTH));
			optionsPanel.add(toDate);
			toDate.addChangeListener( new redrawGraph());
			
			maxLevel = new JTextField("2");
			optionsPanel.add(maxLevel);
			maxLevel.addActionListener( new redrawGraph());

	    }

		return new BookkeepingPage[] 
			{ new BookkeepingPage(swingComposite, "Chart") };
		
		
	}
	
	public class redrawGraph implements ChangeListener, ActionListener{
	    public void stateChanged (ChangeEvent e) {
	        redraw();
	    }
	    public void actionPerformed (ActionEvent e) {
	        redraw();
	    }
	    
	    private void redraw() {
	        // chart.setAccount("Dépenses exceptionelles");
	        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
	        chart.setDates((Date) fromDate.getValue(), (Date) toDate.getValue());
	        chart.setMaxLevel(maxLevel.getText());
	        chart.createValues();
	    }
	}
	

}
