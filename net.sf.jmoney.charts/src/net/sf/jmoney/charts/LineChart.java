package net.sf.jmoney.charts;

import java.awt.Color;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;

import org.eclipse.osgi.framework.debug.Debug;

import net.sf.jmoney.model2.*;  

import com.jrefinery.chart.ChartFactory; 
import com.jrefinery.chart.ChartPanel;
import com.jrefinery.chart.JFreeChart;
import com.jrefinery.data.BasicTimeSeries;
import com.jrefinery.data.CategoryDataset;
import com.jrefinery.data.DefaultCategoryDataset;
import com.jrefinery.data.DefaultPieDataset;
import com.jrefinery.data.TimeSeriesCollection;
import com.jrefinery.data.XYDataset;
import com.jrefinery.data.XYSeries;
import com.jrefinery.data.XYSeriesCollection;


/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class LineChart extends JFrame {

    /**
     * Default constructor.
     */
    public LineChart(String title, Session session) {
        super();


        // init the parameters
        initParameters ();
        
        // create a dataset...
        XYSeries timeSeries = createValues (session);

        // set the values
        XYDataset data = new XYSeriesCollection(timeSeries);

        // create the chart...
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title,  // title
                										"Date", // Name of the X-Axis
                										"Amount", // Name of the Y-Axis
                                                       data,                // data
                                                       false                 // include legend
                                                       );

        // set the background color for the chart...
        chart.setBackgroundPaint(Color.yellow);

        
        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        this.getContentPane().add(chartPanel);

        
     }
    
	protected abstract XYSeries createValues(Session session);

    /**
     * Starting point for the panel.
     */
    public void run() {
    	this.pack();
    	this.setVisible(true);
    }
    
    /*
     * initialize the parameters. This initialization will be called before "setValues"
     * @author Faucheux
     */
    protected void initParameters () {};


}
