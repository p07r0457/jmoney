package net.sf.jmoney.charts;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import net.sf.jmoney.model2.Session;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.TimeSeriesCollection;

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
        
        // add a seconde range axis to improve the lisibility
        NumberAxis axis1 = (NumberAxis) ((XYPlot) chart.getPlot()).getRangeAxis();
        NumberAxis axis2 = new NumberAxis("Amount");
        ((XYPlot) chart.getPlot()).setRangeAxis(1, axis2);
        axis2.setRange(axis1.getRange());

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
