package net.sf.jmoney.charts;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.jmoney.model2.Session;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.labels.StandardCategoryLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.BarRenderer;
import org.jfree.data.CategoryDataset;



/**
 * A simple PieChart
 * @author Faucheux
 */
public abstract class StackedChart extends JFrame {

    protected String title;
    protected Session session;
    protected StackedChartParameters params; 

    /**
     * Default constructor.
     */
    public StackedChart(String title, Session session, StackedChartParameters params) {
        this.title = title;
        this.session = session;
        this.params = params;
    }

    public ChartPanel getChartPanel() {
        
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
        return chartPanel;
     }
    
	protected abstract CategoryDataset createValues(Session session);

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
            if (e.getEntity() instanceof CategoryItemEntity) {
            	if (ChartsPlugin.DEBUG) System.out.println("It is in " + ((CategoryItemEntity) e.getEntity()).getSeries());
            }
        }
    }
}
