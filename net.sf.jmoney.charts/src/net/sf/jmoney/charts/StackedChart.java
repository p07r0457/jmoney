package net.sf.jmoney.charts;

import java.awt.Color;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import org.eclipse.osgi.framework.debug.Debug;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPosition;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.BarRenderer;
import org.jfree.data.CategoryDataset;
import org.jfree.ui.TextAnchor;

import net.sf.jmoney.model2.*;



/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class StackedChart extends JFrame {

    /**
     * Default constructor.
     */
    public StackedChart(String[] accounts, Session session) {
        super();
        String title = accounts.toString();


        // init the parameters
        initParameters (accounts, session);
        
        // create a dataset...
        CategoryDataset data = createValues (session);

        // create the chart...
        JFreeChart chart = ChartFactory.createStackedBarChart(title,  // title
                										"Date", // Name of the X-Axis
                										"Amount", // Name of the Y-Axis
                                                       data,                // data
                                                       PlotOrientation.VERTICAL, // Orientation
                                                       true,                 // include legend
                                                       true,	// tooltips
                                                       true   // URL

                                                       );

        // set the background color for the chart...
        // chart.setBackgroundPaint(Color.yellow);

        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setLabelGenerator(new StackedChart.LabelGenerator());
        renderer.setItemLabelsVisible(true);

        // add the chart to a panel...
        JPanel windowPanel = new JPanel();
        ChartPanel chartPanel = new ChartPanel(chart);
        this.getContentPane().add(chartPanel);
        
        chartPanel.addChartMouseListener(new clickListener());
        
     }
    
	protected abstract CategoryDataset createValues(Session session);

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
    protected void initParameters (String[] accounts, Session session) {};

    
    /**
     * A custom label generator.
     * (copied from BarChartDemo7)
     */
    static class LabelGenerator extends StandardCategoryLabelGenerator {
        /**
         * Generates an item label.
         * 
         * @param dataset  the dataset.
         * @param series  the series index.
         * @param category  the category index.
         * 
         * @return the label.
         */
        public String generateItemLabel(CategoryDataset dataset, 
                                        int series, 
                                        int category) {
            return dataset.getRowKey(series).toString();
        }
    }
    
    private class clickListener implements ChartMouseListener {
        public void chartMouseMoved (ChartMouseEvent e) {}
        public void chartMouseClicked (ChartMouseEvent e) {
            System.out.println("Clicked in " + e.getEntity());
            if (e.getEntity() instanceof CategoryItemEntity) {
                System.out.println("It is in " + ((CategoryItemEntity) e.getEntity()).getSeries());
            }
        }
    }
}
