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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jmoney.Constants;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.VerySimpleDateFormat;

import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertyNotFoundException;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;

import dori.jasper.engine.JRDataSource;
import dori.jasper.engine.JasperFillManager;
import dori.jasper.engine.JasperPrint;
import dori.jasper.engine.data.JRBeanCollectionDataSource;
import dori.jasper.view.JRViewer;

public class AccountBalancesReportPanel extends JPanel implements Constants {

	public static final int ALL_ENTRIES = 0;

	public static final int CLEARED_ENTRIES = 1;

	public static final int DATE = 2;

	public static int[] filterIndexes;

	public static String[] filters;

	public static PropertyAccessor reconciliationStatusAccessor;
	
	static {
		try {
			reconciliationStatusAccessor = PropertySet.getPropertyAccessor("net.sf.jmoney.reconciliation.entryFields.status");
		} catch (PropertyNotFoundException e) {
			// The reconciliation plug-in is not installed.
			// This does not stop operation of this plug-in.  We just have
			// to remove any operations that require the property from the plug-in.
			reconciliationStatusAccessor = null;
		}
		
		if (reconciliationStatusAccessor != null) {
			filterIndexes = new int[] {
				ALL_ENTRIES,
				CLEARED_ENTRIES,
				DATE,
			};
			filters = new String[] {
					ReportsPlugin.getResourceString("Report.AccountBalances.AllEntries"),
					ReportsPlugin.getResourceString("Report.AccountBalances.ClearedEntries"),
					ReportsPlugin.getResourceString("Entry.date"),
			};
		} else {
			filterIndexes = new int[] {
					ALL_ENTRIES,
					DATE,
			};
			filters = new String[] {
					ReportsPlugin.getResourceString("Report.AccountBalances.AllEntries"),
					ReportsPlugin.getResourceString("Entry.date"),
			};
		}
	}
		
	private JPanel reportPanel;
	private JPanel controlPanel = new JPanel();
	private JButton generateButton = new JButton();
	private JLabel filterLabel = new JLabel();
	private JComboBox filterBox = new JComboBox(filters);
	private JLabel dateLabel = new JLabel();
	private JTextField dateField = new JTextField();

	private Session session;
	private VerySimpleDateFormat dateFormat;
	private Date date;

	public AccountBalancesReportPanel() {
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
	}

	private void generateReport() {
		if (reportPanel != null) {
			remove(reportPanel);
			updateUI();
		}
		try {
			URL url =
				ReportsPlugin.class.getResource("resources/AccountBalances.jasper");
			InputStream is = url.openStream();

			Map params = new HashMap();
			params.put(
				"Title",
				ReportsPlugin.getResourceString("Report.AccountBalances.Title"));
			params.put("Subtitle", getSubtitle());
			params.put("Total", ReportsPlugin.getResourceString("Report.Total"));
			params.put(
				"CapitalAccount",
				ReportsPlugin.getResourceString("Report.AccountBalances.CapitalAccount"));
			params.put(
				"Balance",
				ReportsPlugin.getResourceString("Report.AccountBalances.Balance"));
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

	private String getSubtitle() {
		int selectedIndex = filterBox.getSelectedIndex();
		if (selectedIndex >= 0) {
			switch (filterIndexes[selectedIndex]) {
				case ALL_ENTRIES :
					return ReportsPlugin.getResourceString("Report.AccountBalances.AllEntries");
				case CLEARED_ENTRIES :
					return ReportsPlugin.getResourceString(
					"Report.AccountBalances.ClearedEntries");
				case DATE :
					return dateFormat.format(date);
			}
		}
		return "";
	}

	private Collection getItems() {
		Vector items = new Vector();

                        // TODO: why is listIterator used here and not
                        // iterator?
//		Iterator aIt = session.getAccounts().listIterator();
		Iterator aIt = session.getCapitalAccountIterator();
		while (aIt.hasNext()) {
			CapitalAccount account = (CapitalAccount) aIt.next();
			long bal = account.getStartBalance();

                        // TODO: why is listIterator used here and not
                        // iterator?
//			Iterator eIt = account.getEntries().listIterator();
			Iterator eIt = account.getEntriesIterator(session);
			while (eIt.hasNext()) {
				Entry e = (Entry) eIt.next();
				if (accept(e))
					bal += e.getAmount();
			}

			items.add(new Item(account, bal));
		}

		Collections.sort(items);
		return items;
	}

	private boolean accept(Entry entry) {
		int selectedIndex = filterBox.getSelectedIndex();
		if (selectedIndex >= 0) {
			switch (filterIndexes[selectedIndex]) {
				case ALL_ENTRIES :
					return true;
				case CLEARED_ENTRIES :
					// This plug-in does not depend on the reconciliation plug-in.
					// Therefore we have no access to classes and constants in the plug-in.
					// This following constant must match that in the plug-in.
					// It should not be possible to get to this case if the
					// property was not found.
					final int CLEARED = 2;
					int status = entry.getIntegerPropertyValue(reconciliationStatusAccessor);
					return status == CLEARED;
				case DATE :
					return acceptTo(entry.getTransaxion().getDate());
			}
		}
		return true;
	}

	private boolean acceptTo(Date d) {
		if (date == null)
			return true;
		if (d == null)
			return false;
		return (d.before(date) || d.equals(date));
	}

	private void updateFilter() {
		dateField.setEnabled(filterBox.getSelectedIndex() == DATE);
	}

	private void updateDate() {
		date = dateFormat.parse(dateField.getText());
		dateField.setText(dateFormat.format(date));
	}

	private void jbInit() throws Exception {
		setLayout(new BorderLayout());

		filterLabel.setText(ReportsPlugin.getResourceString("EntryFilterPanel.filter"));
		filterBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateFilter();
			}
		});

		dateLabel.setText(JMoneyPlugin.getResourceString("Entry.date"));
		dateField.setEnabled(false);
		dateField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateDate();
			}
		});
		dateField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				updateDate();
			}
		});

		generateButton.setText(ReportsPlugin.getResourceString("Panel.Report.Generate"));
		generateButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				generateReport();
			}
		});

		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		controlPanel.setLayout(new GridBagLayout());
		add(controlPanel, BorderLayout.SOUTH);

		controlPanel.add(
			filterLabel,
			new GridBagConstraints(
				0,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 3, 0),
				0,
				0));
		controlPanel.add(
			filterBox,
			new GridBagConstraints(
				1,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 3, 5),
				0,
				0));
		controlPanel.add(
			dateLabel,
			new GridBagConstraints(
				2,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.NONE,
				new Insets(6, 6, 3, 0),
				0,
				0));
		controlPanel.add(
			dateField,
			new GridBagConstraints(
				3,
				0,
				1,
				1,
				1.0,
				0.0,
				GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL,
				new Insets(6, 6, 3, 5),
				0,
				0));
		controlPanel.add(
			generateButton,
			new GridBagConstraints(
				4,
				0,
				1,
				1,
				0.0,
				0.0,
				GridBagConstraints.EAST,
				GridBagConstraints.NONE,
				new Insets(6, 12, 3, 4),
				0,
				0));
	}

	public class Item implements Comparable {

		private CapitalAccount account;

		private long balance;

		public Item(CapitalAccount anAccount, long aBalance) {
			account = anAccount;
			balance = aBalance;
		}

		public CapitalAccount getAccount() {
			return account;
		}

		public String getAccountName() {
			return account.getName();
		}

		public Long getBalance() {
			return new Long(balance);
		}

		public String getBalanceString() {
			return account.getCurrency().format(balance);
		}

		public void addToBalance(long amount) {
		}

		public String getCurrencyCode() {
			return account.getCurrency().getCode();
		}

		public int compareTo(Object o) {
			return account.compareTo(((Item) o).getAccount());
		}
	}
}