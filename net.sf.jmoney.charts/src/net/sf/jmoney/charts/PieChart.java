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
import com.jrefinery.data.DefaultPieDataset;


/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class PieChart extends JFrame {

    /**
     * Default constructor.
     */
    public PieChart(String title, Session session) {
        super();

        // create a dataset...
        DefaultPieDataset data = new DefaultPieDataset();

        // init the parameters
        initParameters ();
        
        // set the values
        setValues (session, data);

        // create the chart...
        JFreeChart chart = ChartFactory.createPieChart(title,  // chart title
                                                       data,                // data
                                                       true                 // include legend
                                                       );

        // set the background color for the chart...
        chart.setBackgroundPaint(Color.yellow);

        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        this.getContentPane().add(chartPanel);

        
     }
    
	protected abstract void setValues(Session session, DefaultPieDataset data);

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
