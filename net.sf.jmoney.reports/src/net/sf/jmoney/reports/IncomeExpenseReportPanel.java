/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
*
*
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package net.sf.jmoney.reports;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jmoney.Constants;
import net.sf.jmoney.VerySimpleDateFormat;
import net.sf.jmoney.model2.*;

import dori.jasper.engine.JRDataSource;
import dori.jasper.engine.JasperFillManager;
import dori.jasper.engine.JasperPrint;
import dori.jasper.engine.data.JRBeanCollectionDataSource;
import dori.jasper.view.JRViewer;

public class IncomeExpenseReportPanel extends JPanel implements Constants {

	public static final int THIS_MONTH = 0;

	public static final int THIS_YEAR = 1;

	public static final int LAST_MONTH = 2;

	public static final int LAST_YEAR = 3;

	public static final int CUSTOM = 4;

	public static final String[] periods =
		{
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.thisMonth"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.thisYear"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.lastMonth"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.lastYear"),
			ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.custom")};

	private JPanel reportPanel;
	private JPanel controlPanel = new JPanel();
	private JButton generateButton = new JButton();
	private JLabel periodLabel = new JLabel();
	private JComboBox periodBox = new JComboBox(periods);
	private JLabel fromLabel = new JLabel();
	private JTextField fromField = new JTextField();
	private JLabel toLabel = new JLabel();
	private JTextField toField = new JTextField();
	private JCheckBox subtotalsCheckBox = new JCheckBox();

	private Session session;
	private VerySimpleDateFormat dateFormat;
	private Date fromDate;
	private Date toDate;

	public IncomeExpenseReportPanel() {
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setSession(Session aSession) {
		session = aSession;
	}

	public void setDateFormat(String pattern) {
		dateFormat = new VerySimpleDateFormat(pattern);
		updateFromAndTo();
	}

	private void generateReport() {
		if (reportPanel != null) {
			remove(reportPanel);
			updateUI();
		}
		try {
			String reportFile =
				subtotalsCheckBox.isSelected()
					? "resources/IncomeExpenseSubtotals.jasper"
					: "resources/IncomeExpense.jasper";
			URL url = ReportsPlugin.class.getResource(reportFile);
			InputStream is = url.openStream();

			Map params = new HashMap();
			params.put(
				"Title",
				ReportsPlugin.getResourceString("Report.IncomeExpense.Title"));
			params.put(
				"Subtitle",
				dateFormat.format(fromDate)
					+ " - "
					+ dateFormat.format(toDate));
			params.put("Total", ReportsPlugin.getResourceString("Report.Total"));
			params.put("Category", ReportsPlugin.getResourceString("Entry.category"));
			params.put(
				"Income",
				ReportsPlugin.getResourceString("Report.IncomeExpense.Income"));
			params.put(
				"Expense",
				ReportsPlugin.getResourceString("Report.IncomeExpense.Expense"));
			params.put("DateToday", dateFormat.format(new Date()));
			params.put("Page", ReportsPlugin.getResourceString("Report.Page"));

			Collection items = getItems();
			if (items.isEmpty()) {
				JOptionPane.showMessageDialog(
					this,
					ReportsPlugin.getResourceString("Panel.Report.EmptyReport.Message"),
					ReportsPlugin.getResourceString("Panel.Report.EmptyReport.Title"),
					JOptionPane.ERROR_MESSAGE);
			} else {
				JRDataSource ds = new JRBeanCollectionDataSource(getItems());
				JasperPrint print =
					JasperFillManager.fillReport(is, params, ds);
				reportPanel = new JRViewer(print);
				add(reportPanel, BorderLayout.CENTER);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		updateUI();
	}

	private Collection getItems() {
		Vector allItems = new Vector();
		HashMap byCurrency = new HashMap();

		Iterator aIt = session.getCapitalAccountIterator();
		while (aIt.hasNext()) {
			CapitalAccount a = (CapitalAccount) aIt.next();
			String cc = a.getCurrency().getCode();
			HashMap items = (HashMap) byCurrency.get(cc);
			if (items == null) {
				items = new HashMap();
				byCurrency.put(cc, items);
			}
			addEntries(allItems, items, cc, a.getEntriesIterator(session));
		}

		Collections.sort(allItems);
		return allItems;
	}

	private void addEntries(
		Vector allItems,
		HashMap items,
		String currencyCode,
		Iterator eIt) {
		while (eIt.hasNext()) {
			Entry e = (Entry) eIt.next();
			if (!accept(e))
				continue;
/*
			if (e instanceof SplittedEntry) {
				addEntries(
					allItems,
					items,
					currencyCode,
					((SplittedEntry) e).getEntries());
			} else {
				Category c = e.getCategory();
				Item i = (Item) items.get(e.getCategory());
				if (i == null) {
					i = new Item(e.getCategory(), currencyCode, e.getAmount());
					items.put(e.getCategory(), i);
					allItems.add(i);
				} else {
					i.addToSum(e.getAmount());
				}
			}
*/
                        for (Iterator iter = e.getTransaxion().getEntryIterator(); iter.hasNext(); ) {
                            Entry e2 = (Entry)iter.next();
                            if (e2 != e) {
				Account c = e.getAccount();
				Item i = (Item) items.get(e2.getAccount());
				if (i == null) {
					i = new Item(e2.getAccount(), currencyCode, e2.getAmount());
					items.put(e.getAccount(), i);
					allItems.add(i);
				} else {
					i.addToSum(e2.getAmount());
				}
                            }
			}
		}
	}

	private boolean accept(Entry e) {
            // TODO: figure out this code
//		if (e instanceof DoubleEntry)
//			return false;
		return acceptFrom(e.getTransaxion().getDate()) && acceptTo(e.getTransaxion().getDate());
	}

	private boolean acceptFrom(Date d) {
		if (fromDate == null)
			return false;
		if (d == null)
			return true;
		return (d.after(fromDate) || d.equals(fromDate));
	}

	private boolean acceptTo(Date d) {
		if (toDate == null)
			return true;
		if (d == null)
			return false;
		return (d.before(toDate) || d.equals(toDate));
	}

	private void updateFromAndTo() {
		int index = periodBox.getSelectedIndex();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		switch (index) {
			case THIS_MONTH :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				fromDate = cal.getTime();

				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case THIS_YEAR :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				fromDate = cal.getTime();

				cal.add(Calendar.YEAR, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case LAST_MONTH :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.add(Calendar.MONTH, -1);
				fromDate = cal.getTime();

				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case LAST_YEAR :
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				cal.add(Calendar.YEAR, -1);
				fromDate = cal.getTime();

				cal.add(Calendar.YEAR, 1);
				cal.add(Calendar.MILLISECOND, -1);
				toDate = cal.getTime();
				break;
			case CUSTOM :
			default :
				}

		fromField.setText(dateFormat.format(fromDate));
		fromField.setEnabled(index == CUSTOM);
		toField.setText(dateFormat.format(toDate));
		toField.setEnabled(index == CUSTOM);
	}

	private void updateFrom() {
		fromDate = dateFormat.parse(fromField.getText());
		fromField.setText(dateFormat.format(fromDate));
	}

	private void updateTo() {
		toDate = dateFormat.parse(toField.getText());
		toField.setText(dateFormat.format(toDate));
	}

	private void jbInit() throws Exception {
		setLayout(new BorderLayout());
		periodLabel.setText(
				ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.Period"));
		periodBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFromAndTo();
			}
		});
		fromLabel.setText(
				ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.From"));
		fromField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFrom();
			}
		});
		fromField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateFrom();
			}
		});
		toLabel.setText(ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.To"));
		toField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateTo();
			}
		});
		fromField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateTo();
			}
		});
		generateButton.setText(ReportsPlugin.getResourceString("Panel.Report.Generate"));
		generateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				generateReport();
			}
		});

		subtotalsCheckBox.setText(
				ReportsPlugin.getResourceString("Panel.Report.IncomeExpense.ShowSubtotals"));

		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		controlPanel.setLayout(new GridBagLayout());
		controlPanel.add(
			periodLabel,
			new GridBagConstraints(
				0,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 0, 0),
				0,
				0));
		controlPanel.add(
			periodBox,
			new GridBagConstraints(
				1,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 0, 5),
				0,
				0));
		controlPanel.add(
			fromLabel,
			new GridBagConstraints(
				2,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 0, 0),
				0,
				0));
		controlPanel.add(
			fromField,
			new GridBagConstraints(
				3,
				0,
				1,
				1,
				1.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL,
				new Insets(6, 6, 0, 5),
				0,
				0));
		controlPanel.add(
			toLabel,
			new GridBagConstraints(
				4,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 0, 0),
				0,
				0));
		controlPanel.add(
			toField,
			new GridBagConstraints(
				5,
				0,
				1,
				1,
				1.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL,
				new Insets(6, 6, 0, 5),
				0,
				0));
		controlPanel.add(
			generateButton,
			new GridBagConstraints(
				6,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.EAST,
				GridBagConstraints.NONE,
				new Insets(6, 12, 0, 4),
				0,
				0));
		controlPanel.add(
			subtotalsCheckBox,
			new GridBagConstraints(
				0,
				1,
				6,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(0, 6, 0, 0),
				0,
				0));
		add(controlPanel, BorderLayout.SOUTH);
	}

	public class Item implements Comparable {

		private Account category;
		private String currencyCode;
		private long sum;

		public Item(Account aCategory, String aCurrencyCode, long aSum) {
			category = aCategory;
			currencyCode = aCurrencyCode;
			sum = aSum;
		}

		public String getCurrencyCode() {
			return currencyCode;
		}

		public String getBaseCategory() {
			if (category == null)
				return ReportsPlugin.getResourceString("Report.IncomeExpense.NoCategory");
                        Account baseCategory = category;
                        while (baseCategory.getParent() != null) {
                            baseCategory = baseCategory.getParent();
                        }
			return baseCategory.getName();
		}

		public String getCategory() {
			return category == null
				? ReportsPlugin.getResourceString("Report.IncomeExpense.NoCategory")
				: category.getFullAccountName();
		}

		public Long getIncome() {
			return sum >= 0 ? new Long(sum) : null;
		}

		public String getIncomeString() {
                    return formatAmount(currencyCode, getIncome());
		}

		public Long getExpense() {
			return sum < 0 ? new Long(-sum) : null;
		}

		public String getExpenseString() {
                    return formatAmount(currencyCode, getExpense());
                }
                
                private String formatAmount(String currencyCode, Long amount) {
                    if (amount == null) {
                        return "";
                    } else {
			return session.getCurrencyForCode(currencyCode).format(
				amount.longValue());
                    }
		}

		public void addToSum(long amount) {
			sum += amount;
		}

		public boolean noCategory() {
			return category == null;
		}

		public int compareTo(Object o) {
			Item other = (Item) o;
			if (noCategory() && other.noCategory())
				return 0;
			else if (noCategory())
				return 1;
			else if (other.noCategory())
				return -1;
			else
				return getCategory().compareTo(other.getCategory());
		}
	}
}