package net.sf.jmoney.charts;

import java.awt.Color;
import java.awt.Frame;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;

import org.eclipse.osgi.framework.debug.Debug;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.DefaultPieDataset;
import org.jfree.data.PieDataset;
import org.jfree.ui.*;

import net.sf.jmoney.model2.*;


/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class PieChart extends JFrame implements ChartMouseListener {

    protected String title;
    protected List subTitle;
    protected Session session;
    public DefaultPieDataset data;
    protected JFreeChart chart;
    
    /**
     * Default constructor.
     */
    public PieChart(String title, Session session) {
        super();

        this.title = title;
        this.session = session;
       
    }
    
    /**
     * Starting point for the panel.
     */
    public void run() {
        // create a dataset...
        data = new DefaultPieDataset();

        // init the parameters
        initParameters ();
        
        // set the values
        createValues ();

        /*
        // create the chart...
         chart = ChartFactory.createPieChart(title,  // chart title
                                                       data,   // data
                                                       true,    // include legend
                                                       false,	// tooltips
                                                       false);   // URL

        // set the background color for the chart...
        chart.setBackgroundPaint(Color.yellow);

        
        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.addChartMouseListener(this);
        this.getContentPane().add(chartPanel);
        this.pack();
        this.setVisible(true);
        */
        
    }
    
    /**
     * initialize the parameters. This initialization will be called before "setValues"
     * @author Faucheux
     */
    protected void initParameters () {};

    public ChartPanel getChartPanel() {

        // create the chart...
        chart = ChartFactory.createPieChart(title,  // chart title
                                                       data,   // data
                                                       true,    // include legend
                                                       false,	// tooltips
                                                       false);   // URL
        chart.setBorderVisible(true);
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setStartAngle(60);


        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        
        return chartPanel;
    }
    
	protected abstract void createValues();

	public abstract void chartMouseClicked (ChartMouseEvent e);
	public void chartMouseMoved(ChartMouseEvent e) { 
	    /* Nothing */
	}

}
