package net.sf.jmoney.charts;

import javax.swing.JFrame;

import net.sf.jmoney.model2.Session;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.DefaultPieDataset;


/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class PieChart extends JFrame implements ChartMouseListener {

    protected String title;
    protected String subTitle;
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

        // create the chart...
         chart = ChartFactory.createPieChart(title,  // chart title
                                                       data,   // data
                                                       true,    // include legend
                                                       false,	// tooltips
                                                       false);   // URL
         chart.addSubtitle(new TextTitle(subTitle));
                                                       
        /*
        // set the background color for the chart...
        chart.setBackgroundPaint(Color.yellow);

        */
        
        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.addChartMouseListener(this);
        this.getContentPane().add(chartPanel);
        this.pack();
        this.setVisible(true);
        
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
