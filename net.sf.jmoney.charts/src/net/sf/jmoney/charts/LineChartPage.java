
package net.sf.jmoney.charts;


import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseMotionListener;
import java.awt.peer.PanelPeer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.internal.layout.LayoutUtil;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.data.time.TimeSeries;
import org.jfree.xml.generator.SplittingModelWriter;
import org.w3c.dom.events.MouseEvent;

import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Session;

/**
 * @author Faucheux
 */
public class LineChartPage implements IBookkeepingPageListener {

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
	    final LineChart chart;
	    final java.awt.Panel panel;
	    {
			java.awt.Frame accountEntriesFrame = SWT_AWT.new_Frame(swingComposite);
			panel = new java.awt.Panel(new BorderLayout());
			panel.setBackground(Color.pink);
			
			Panel optionsPanel = new Panel(new FlowLayout());
			panel.add(optionsPanel, BorderLayout.NORTH);
			optionsPanel.setBackground(Color.magenta); 
			
			Panel graphPanel = new Panel (new BorderLayout());
			panel.add(graphPanel, BorderLayout.CENTER);
			
			Button redraw = new Button("Redraw");
			optionsPanel.add(redraw);

			Button root = new Button("back to the root");
			optionsPanel.add(root);
			
			// final TextField fromDate = new TextField("from");    optionsPanel.add(fromDate);
			final JSpinner fromDate = new JSpinner(new SpinnerDateModel(new Date(0), null, null, Calendar.DATE));
			optionsPanel.add(fromDate);
			final JSpinner toDate = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DATE));
			optionsPanel.add(toDate);

			accountEntriesFrame.add(panel);
			chart = new  ActivLineChart("myChart", session);
			chart.run();
			final ChartPanel chartPanel = chart.getChartPanel();
			chartPanel.setPreferredSize(new Dimension(500,270));
			graphPanel.add(chartPanel);

			redraw.addActionListener(new ActionListener () {
			    public void actionPerformed (ActionEvent e) {
			        // chart.setAccount("Dépenses exceptionelles");
			        DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
			        chart.setDates((Date) fromDate.getValue(), (Date) toDate.getValue());
			        chart.createValues();
			    }
			});


			root.addActionListener(new ActionListener () {
			    public void actionPerformed (ActionEvent e) {
			        chart.createValues();
			    }
			});
}

		return new BookkeepingPage[] 
			{ new BookkeepingPage(swingComposite, "Chart") };
	}
}
