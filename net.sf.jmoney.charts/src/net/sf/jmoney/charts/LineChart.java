package net.sf.jmoney.charts;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JWindow;

import org.eclipse.osgi.framework.debug.Debug;

import net.sf.jmoney.model2.*;  


import org.jfree.data.*;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.*;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;

/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class LineChart extends JFrame {

    public String title;
    public Session session;
    public TimeSeriesCollection data;
    protected JFreeChart chart;
    public LineChartParameters params;
    
    /**
     * Default constructor.
     */
    public LineChart(String title, Session session) {
        super();
        this.title = title;
        this.session = session;
        

        // init the parameters
        initParameters ();
        
     }

	protected abstract void createOrUpdateValues(LineChartParameters params);

    /**
     * Starting point for the panel.
     */
    public void displayAsWindow() {
    	this.getContentPane().removeAll();
    	this.getContentPane().setLayout(new BorderLayout());
    	this.getContentPane().add(this.getChartPanel());
    	this.setVisible(true);
    	this.pack();
    }
    
    /*
     * initialize the parameters. This initialization will be called before "setValues"
     * @author Faucheux
     */
    protected void initParameters () {};

    public ChartPanel getChartPanel() {

        // create the chart...
         chart = ChartFactory.createTimeSeriesChart(title,  // title
                										"Date", // Name of the X-Axis
                										"Amount", // Name of the Y-Axis
                                                       data,                // data
                                                       true,                 // include legend
                                                       true,	// tooltips
                                                       true   // URL
                                                       );

        // set the background color for the chart...
        chart.setBackgroundPaint(Color.yellow);
        

        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        
        // made the 0 line to ressort.
        chart.getXYPlot().addRangeMarker(new ValueMarker(0.0));

        JSplitPane splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitter.setTopComponent(chartPanel);
        splitter.setBottomComponent(chartPanel);
        this.getContentPane().add(splitter);
        splitter.setDividerLocation(0.75);

        return chartPanel;
    }

}
