/*
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2004 Johann Gyger <jgyger@users.sf.net>
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
 */
package net.sf.jmoney.stocks.pages;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.entrytable.BalanceColumn;
import net.sf.jmoney.entrytable.BaseEntryRowControl;
import net.sf.jmoney.entrytable.Block;
import net.sf.jmoney.entrytable.CellBlock;
import net.sf.jmoney.entrytable.DebitAndCreditColumns;
import net.sf.jmoney.entrytable.EntriesTable;
import net.sf.jmoney.entrytable.EntryData;
import net.sf.jmoney.entrytable.EntryRowControl;
import net.sf.jmoney.entrytable.HorizontalBlock;
import net.sf.jmoney.entrytable.ICellControl;
import net.sf.jmoney.entrytable.IEntriesContent;
import net.sf.jmoney.entrytable.IRowProvider;
import net.sf.jmoney.entrytable.IndividualBlock;
import net.sf.jmoney.entrytable.OtherEntriesBlock;
import net.sf.jmoney.entrytable.PropertyBlock;
import net.sf.jmoney.entrytable.RowSelectionTracker;
import net.sf.jmoney.entrytable.SingleOtherEntryPropertyBlock;
import net.sf.jmoney.entrytable.SplitEntryRowControl;
import net.sf.jmoney.entrytable.StackBlock;
import net.sf.jmoney.entrytable.StackControl;
import net.sf.jmoney.entrytable.VerticalBlock;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.Currency;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.EntryInfo;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.stocks.Stock;
import net.sf.jmoney.stocks.StockAccount;
import net.sf.jmoney.stocks.StockControl;
import net.sf.jmoney.stocks.StockEntry;
import net.sf.jmoney.stocks.StockEntryInfo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * TODO
 * 
 * @author Johann Gyger
 */
public class EntriesSection extends SectionPart implements IEntriesContent {

	private StockAccount account;

    private EntriesTable<StockEntryData> fEntriesControl;
    
    private Block<StockEntryData, StockEntryRowControl> rootBlock;
    
    public EntriesSection(Composite parent, StockAccount account, FormToolkit toolkit) {
        super(parent, toolkit, Section.TITLE_BAR);
        getSection().setText("All Entries");
        this.account = account;
        createClient(toolkit);
    }

    public void refreshEntryList() {
    	fEntriesControl.refreshEntryList();
    }

    protected void createClient(FormToolkit toolkit) {
    	
		/*
		 * Setup the layout structure of the header and rows.
		 */
		IndividualBlock<EntryData, Composite> transactionDateColumn = PropertyBlock.createTransactionColumn(TransactionInfo.getDateAccessor());

		IndividualBlock<StockEntryData, StockEntryRowControl> actionColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Action", 50, 1) {

			@Override
			public ICellControl<StockEntryData> createCellControl(Composite parent, final StockEntryRowControl rowControl) {
				final CCombo control = new CCombo(parent, SWT.NONE);
				control.add("buy");
				control.add("sell");
				control.add("dividend");
				control.add("transfer");
				control.add("custom");
				
				control.addSelectionListener(new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						int index = control.getSelectionIndex();
						switch (index) {
						case 0:
							rowControl.getUncommittedEntryData().forceTransactionToBuy();
							break;
						case 1:
							rowControl.getUncommittedEntryData().forceTransactionToSell();
							break;
						case 2:
							rowControl.getUncommittedEntryData().forceTransactionToDividend();
							break;
						case 3:
							rowControl.getUncommittedEntryData().forceTransactionToTransfer();
							break;
						case 4:
							rowControl.getUncommittedEntryData().forceTransactionToCustom();
							break;
						}
						
						rowControl.fireTransactionTypeChange();
					}
				});
				
				return new ICellControl<StockEntryData>() {

					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						if (data.getTransactionType() == null) {
							control.deselectAll();
							control.setText("");
						} else {
							switch (data.getTransactionType()) {
							case Buy:
								control.select(0);
								break;
							case Sell:
								control.select(1);
								break;
							case Dividend:
								control.select(2);
								break;
							case Transfer:
								control.select(3);
								break;
							case Other:
								control.select(4);
								break;
							default:
								throw new RuntimeException("bad case");
							}
						}
					}

					public void save() {
						// TODO Auto-generated method stub
						
					}

					public void setFocusListener(FocusListener controlFocusListener) {
						// TODO Auto-generated method stub
						
					}
				};
			}
		};  

		IndividualBlock<StockEntryData, StockEntryRowControl> shareNameColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Stock", 50, 1) {

			@Override
			public ICellControl<StockEntryData> createCellControl(Composite parent, final StockEntryRowControl rowControl) {
				final StockControl<Stock> control = new StockControl<Stock>(parent, null, Stock.class);
				
				ICellControl<StockEntryData> cellControl = new ICellControl<StockEntryData>() {
					private StockEntryData data;
					
					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						this.data = data;
						
						/*
						 * We have to find the appropriate entry in the transaction that contains
						 * the stock.
						 * 
						 * - If this is a purchase or sale, then the stock will be set as the commodity
						 * for one of the entries.  We find this entry.
						 * - If this is a dividend payment then the stock will be set as an additional
						 * field in the dividend category. 
						 */
						Stock stock;
						if (data.isPurchaseOrSale()) {
							Entry entry = data.getPurchaseOrSaleEntry();
							stock = (Stock)entry.getCommodity();
						} else if (data.isDividend()) {
							Entry entry = data.getDividendEntry();
							stock = entry.getPropertyValue(StockEntryInfo.getStockAccessor());
						} else {
							stock = null;
							control.setEnabled(false);
						}
						
				        control.setSession(data.getEntry().getSession(), Stock.class);
						
						control.setStock(stock);
					}

					public void save() {
						Stock stock = control.getStock();
					
						if (data.isPurchaseOrSale()) {
							Entry entry = data.getPurchaseOrSaleEntry();
							StockEntry stockEntry = entry.getExtension(StockEntryInfo.getPropertySet(), true);
							stockEntry.setStockChange(true);
							stockEntry.setStock(stock);
						} else if (data.isDividend()) {
							Entry entry = data.getDividendEntry();
							entry.setPropertyValue(StockEntryInfo.getStockAccessor(), stock);
						}
					}

					public void setFocusListener(FocusListener controlFocusListener) {
						// TODO Auto-generated method stub
						
					}
				};

				rowControl.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {

					public void transactionTypeChanged() {
						/*
						 * If the user changes the transaction type, the stock control remains
						 * the same as it was in the previous transaction type.
						 * 
						 * For example, suppose an entry is a purchase of stock in Foo company.
						 * The user changes the entry to a dividend.  The entry will then
						 * be a dividend from stock in Foo company.  The user changes the stock
						 * to Bar company.  Then the user changes the transaction type back
						 * to a purchase.  The entry will now show a purchase of stock in Bar
						 * company.
						 */
						Stock stock = control.getStock();
						if (rowControl.getUncommittedEntryData().isPurchaseOrSale()) {
							Entry entry = rowControl.getUncommittedEntryData().getPurchaseOrSaleEntry();
							entry.setPropertyValue(StockEntryInfo.getStockAccessor(), stock);
							control.setEnabled(true);
						} else if (rowControl.getUncommittedEntryData().isDividend()) {
							Entry entry = rowControl.getUncommittedEntryData().getDividendEntry();
							entry.setPropertyValue(StockEntryInfo.getStockAccessor(), stock);
							control.setEnabled(true);
						} else {
							stock = null;
							control.setEnabled(false);
						}
					}
				});
				
				return cellControl;
			}
		};  

		IndividualBlock<StockEntryData, StockEntryRowControl> priceColumn = new IndividualBlock<StockEntryData, StockEntryRowControl>("Price", 60, 1) {

			@Override
			public ICellControl<StockEntryData> createCellControl(Composite parent, final StockEntryRowControl rowControl) {
				final Text control = new Text(parent, SWT.RIGHT);
				
				return new ICellControl<StockEntryData>() {

					public Control getControl() {
						return control;
					}

					public void load(StockEntryData data) {
						// The price is calculated.
						assert(data.isPurchaseOrSale());
						long totalShares = data.getPurchaseOrSaleEntry().getAmount();
						long totalCash = 0;
						for (Entry entry: data.getEntry().getTransaction().getEntryCollection()) {
							if (entry.getCommodity() instanceof Currency) {
								totalCash += entry.getAmount();
							}
						}
						if (totalCash != 0 && totalShares != 0) {
							// Either we gain cash and lose stock, or we lose cash
							// and gain stock.  Hence we need to negate to get a
							// positive value.
							double price = -((double)totalCash)/totalShares;
							control.setText(account.getPriceFormatter().format((long)price));
						} else {
							control.setText("");
						}
					}

					public void save() {
						long amount = account.getPriceFormatter().parse(control.getText());
						
						/*
						 * The share price is a calculated amount so is not
						 * stored in any property. However, we do tell the row
						 * control because it needs to save the value so it can
						 * be checked for consistency when the transaction is
						 * saved and also because other values may need to be
						 * adjusted as a result of the new share price.
						 */
						rowControl.sharePriceChanged(amount);
					}

					public void setFocusListener(FocusListener controlFocusListener) {
						control.addFocusListener(controlFocusListener);
					}
				};
			}
		};  

		
		IndividualBlock<StockEntryData, StockEntryRowControl> shareNumberColumn = new PropertyBlock<StockEntryData, StockEntryRowControl>(EntryInfo.getAmountAccessor(), "shareNumber", "Quantity") {
			@Override
			public ExtendableObject getObjectContainingProperty(StockEntryData data) {
				return data.getPurchaseOrSaleEntry();
			}
			@Override
			public void fireUserChange(StockEntryRowControl rowControl) {
				rowControl.quantityChanged();
			}
		};		
		
		final IndividualBlock<StockEntryData, Composite> withholdingTaxColumn = new PropertyBlock<StockEntryData, Composite>(EntryInfo.getAmountAccessor(), "withholdingTax", "Withholding Tax") {
			@Override
			public ExtendableObject getObjectContainingProperty(StockEntryData data) {
				return data.getWithholdingTaxEntry();
			}
		};		

		ArrayList<Block<? super StockEntryData, ? super StockEntryRowControl>> expenseColumns = new ArrayList<Block<? super StockEntryData, ? super StockEntryRowControl>>();
		
		IndividualBlock<StockEntryData, Composite> commissionColumn = new PropertyBlock<StockEntryData, Composite>(EntryInfo.getAmountAccessor(), "commission", "Commission") {
			@Override
			public ExtendableObject getObjectContainingProperty(StockEntryData data) {
				return data.getCommissionEntry();
			}
		};		
		expenseColumns.add(commissionColumn);
		
		if (account.getTax1Name() != null) {
			IndividualBlock<StockEntryData, Composite> tax1Column = new PropertyBlock<StockEntryData, Composite>(EntryInfo.getAmountAccessor(), "tax1", account.getTax1Name()) {
				@Override
				public ExtendableObject getObjectContainingProperty(StockEntryData data) {
					return data.getTax1Entry();
				}
			};
			expenseColumns.add(tax1Column);
		}
		
		if (account.getTax2Name() != null) {
			IndividualBlock<StockEntryData, Composite> tax2Column = new PropertyBlock<StockEntryData, Composite>(EntryInfo.getAmountAccessor(), "tax2", account.getTax2Name()) {
				@Override
				public ExtendableObject getObjectContainingProperty(StockEntryData data) {
					return data.getTax2Entry();
				}
			};
			expenseColumns.add(tax2Column);
		}
		
		final Block<StockEntryData, StockEntryRowControl> purchaseOrSaleInfoColumn = new VerticalBlock<StockEntryData, StockEntryRowControl>(
				// TEMP
				new VerticalBlock<StockEntryData, StockEntryRowControl>(
						priceColumn,
						shareNumberColumn
				),
				new HorizontalBlock<StockEntryData, StockEntryRowControl>(
						expenseColumns
				)
		);

		final IndividualBlock<StockEntryData, Composite> transferAccountColumn = new PropertyBlock<StockEntryData, Composite>(EntryInfo.getAccountAccessor(), "transferAccount", "Transfer Account") {
			@Override
			public ExtendableObject getObjectContainingProperty(StockEntryData data) {
				return data.getTransferEntry();
			}
		};		

		final Block<EntryData, BaseEntryRowControl> customTransactionColumn = new OtherEntriesBlock(
				new HorizontalBlock<Entry, SplitEntryRowControl>(
						new SingleOtherEntryPropertyBlock(EntryInfo.getAccountAccessor()),
						new SingleOtherEntryPropertyBlock(EntryInfo.getMemoAccessor(), JMoneyPlugin.getResourceString("Entry.description")),
						new SingleOtherEntryPropertyBlock(EntryInfo.getAmountAccessor())
				)
		);
		
		CellBlock<EntryData, BaseEntryRowControl> debitColumnManager = DebitAndCreditColumns.createDebitColumn(account.getCurrency());
		CellBlock<EntryData, BaseEntryRowControl> creditColumnManager = DebitAndCreditColumns.createCreditColumn(account.getCurrency());
    	CellBlock<EntryData, BaseEntryRowControl> balanceColumnManager = new BalanceColumn(account.getCurrency());
		
		RowSelectionTracker<EntryRowControl> rowSelectionTracker = new RowSelectionTracker<EntryRowControl>();

		rootBlock = new HorizontalBlock<StockEntryData, StockEntryRowControl>(
				transactionDateColumn,
				new VerticalBlock<StockEntryData, StockEntryRowControl>(
						new HorizontalBlock<StockEntryData, StockEntryRowControl>(
								actionColumn,
								shareNameColumn
						),
						PropertyBlock.createEntryColumn(EntryInfo.getMemoAccessor())
				),
				new StackBlock<StockEntryData, StockEntryRowControl>(
						withholdingTaxColumn,
						purchaseOrSaleInfoColumn,
						transferAccountColumn,
						customTransactionColumn
				) {

					@Override
					protected Block<? super StockEntryData, ? super StockEntryRowControl> getTopBlock(StockEntryData data) {
						if (data.getTransactionType() == null) {
							return null;
						} else {
							switch (data.getTransactionType()) {
							case Buy:
							case Sell:
								return purchaseOrSaleInfoColumn;
							case Dividend:
								return withholdingTaxColumn;
							case Transfer:
								return transferAccountColumn;
							case Other:
								return customTransactionColumn;
							default:
								throw new RuntimeException("bad case");
							}
						}
					}
					
				    @Override	
					public ICellControl<StockEntryData> createCellControl(Composite parent, final StockEntryRowControl rowControl) {
						final StackControl<StockEntryData, StockEntryRowControl> control = new StackControl<StockEntryData, StockEntryRowControl>(parent, rowControl, this);
						
						rowControl.addTransactionTypeChangeListener(new ITransactionTypeChangeListener() {

							@Override
							public void transactionTypeChanged() {
								Block<? super StockEntryData, ? super StockEntryRowControl> topBlock = getTopBlock(rowControl.getUncommittedEntryData());
								
								// Set this block in the control
								control.setTopBlock(topBlock);

								/*
								 * This stack layout has a size this is the
								 * preferred size of the top control, ignoring
								 * all the other controls. Therefore changing
								 * the top control may change the size of the
								 * row.
								 */
								fEntriesControl.table.refreshSize(rowControl);
								
								/*
								 * The above method will re-size the height of the row
								 * to its preferred height, but it won't layout the child
								 * controls if the preferred height did not change.
								 * We therefore force a layout in order to bring the new
								 * top control to the top and layout its child controls.
								 */
								rowControl.layout(true);
							}
						});
						
						return control;
				    }

					@Override
					public SessionChangeListener createListener(
							final StockEntryData entryData,
							final StackControl<StockEntryData, StockEntryRowControl> stackControl) {
						return 	new SessionChangeAdapter() {
							@Override
							public void objectChanged(ExtendableObject changedObject,
									ScalarPropertyAccessor changedProperty, Object oldValue,
									Object newValue) {
								// TODO Auto-generated method stub
								
							}

							@Override
							public void objectCreated(ExtendableObject newObject) {
								// TODO Auto-generated method stub
								
							}

							@Override
							public void objectDestroyed(ExtendableObject deletedObject) {
								// TODO Auto-generated method stub
								
							}

							@Override
							public void objectInserted(ExtendableObject newObject) {
								// TODO Auto-generated method stub
								
							}

							@Override
							public void objectMoved(ExtendableObject movedObject,
									ExtendableObject originalParent, ExtendableObject newParent,
									ListPropertyAccessor originalParentListProperty,
									ListPropertyAccessor newParentListProperty) {
								// TODO Auto-generated method stub
								
							}

							@Override
							public void objectRemoved(ExtendableObject deletedObject) {
								// TODO Auto-generated method stub
								
							}

							@Override
							public void performRefresh() {
								// TODO Auto-generated method stub
								
							}
						};
					}
						
				},
				debitColumnManager,
				creditColumnManager,
				balanceColumnManager
		);

		// Create the table control.
	    IRowProvider<StockEntryData> rowProvider = new StockRowProvider(rootBlock);
		fEntriesControl = new EntriesTable<StockEntryData>(getSection(), toolkit, rootBlock, this, rowProvider, account.getSession(), transactionDateColumn, rowSelectionTracker) {
			@Override
			protected StockEntryData createEntryRowInput(Entry entry) {
				return new StockEntryData(entry, session.getDataManager());
			}

			@Override
			protected StockEntryData createNewEntryRowInput() {
				return new StockEntryData(null, session.getDataManager());
			}
		}; 
			
        getSection().setClient(fEntriesControl);
        toolkit.paintBordersFor(fEntriesControl);
        refresh();
    }

    /**
     * @return the entries to be shown in the table, unsorted
     */
	public Collection<Entry> getEntries() {
		/*
		 * We want only cash entries, not stock entries.  This is providing
		 * content for a table of entries that show the running balance.
		 * A stock entry or an entry in a currency other than the currency
		 * of the account should not be returned.
		 */
		Collection<Entry> entries = new ArrayList<Entry>();
		for (Entry entry : account.getEntries()) {
			if (entry.getCommodity() == account.getCurrency()) {
				entries.add(entry);
			}
		}
		
		return entries;
	}

	public boolean isEntryInTable(Entry entry) {
		return account.equals(entry.getAccount());
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#filterEntry(net.sf.jmoney.pages.entries.EntriesTable.DisplayableTransaction)
	 */
	public boolean filterEntry(EntryData data) {
		return true;
	}

	/* (non-Javadoc)
	 * @see net.sf.jmoney.pages.entries.IEntriesContent#getStartBalance()
	 */
	public long getStartBalance() {
        return 0; 
        // ???? account.getStartBalance();
	}

	public Entry createNewEntry(Transaction newTransaction) {
		/*
		 * For stock entries, we create a single entry only.
		 * The other entries are created as appropriate when a
		 * transaction type is selected.
		 */
		Entry entryInTransaction = newTransaction.createEntry();

		// It is assumed that the entry is in a data manager that is a direct
		// child of the data manager that contains the account.
		TransactionManager tm = (TransactionManager)entryInTransaction.getDataManager();
		entryInTransaction.setAccount(tm.getCopyInTransaction(account));

		return entryInTransaction;
	}
}