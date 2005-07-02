
package net.sf.jmoney.charts;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.jfree.chart.ChartPanel;

/**
 * @author Faucheux
 */
public class PieChartPage implements IBookkeepingPageFactory {

    private static final String PAGE_ID = "net.sf.jmoney.charts.pieChart";
    
    protected ExpensePieChart chart;
    protected JSpinner fromDate;
    protected JSpinner toDate;
    protected JTextField maxLevel;
    
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageFactory#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public Composite createContent(Session session, Composite parent) {

	    
	    
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

		return swingComposite;
		
		
	}
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
		SectionlessPage formPage = new SectionlessPage(
				editor,
				PAGE_ID, 
				"Chart", 
				"Pie Chart") {
			
			public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
				Session session = JMoneyPlugin.getDefault().getSession();
				return createContent(session, parent);
			}

			public void saveState(IMemento memento) {
				// TODO Save the chart options selected by the user
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
	
	public class redrawGraph implements ChangeListener, ActionListener{
	    public void stateChanged (ChangeEvent e) {
	        redraw();
	    }
	    public void actionPerformed (ActionEvent e) {
	        redraw();
	    }
	    
	    private void redraw() {
	        // chart.setAccount("Dépenses exceptionelles");
	        chart.setDates((Date) fromDate.getValue(), (Date) toDate.getValue());
	        chart.setMaxLevel(maxLevel.getText());
	        chart.createValues();
	    }
	}
	

}
