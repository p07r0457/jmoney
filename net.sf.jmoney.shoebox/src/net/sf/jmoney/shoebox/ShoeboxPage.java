/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *  Copyright (c) 2006 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.shoebox;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.ITransactionTemplate;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.fields.EntryInfo;
import net.sf.jmoney.fields.TransactionInfo;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.isolation.UncommittedObjectKey;
import net.sf.jmoney.model2.CapitalAccount;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IObjectKey;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.pages.entries.EntriesTree;
import net.sf.jmoney.pages.entries.IDisplayableItem;
import net.sf.jmoney.pages.entries.IEntriesContent;
import net.sf.jmoney.pages.entries.IEntriesTableProperty;
import net.sf.jmoney.pages.entries.EntriesTree.DisplayableTransaction;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Nigel Westbury
 */
public class ShoeboxPage implements IBookkeepingPageFactory {
	
	private static final String PAGE_ID = "net.sf.jmoney.shoebox.editor";
	
	protected Vector<IEntriesTableProperty> allEntryDataObjects = new Vector<IEntriesTableProperty>();

	IEntriesTableProperty debitColumnManager;
	IEntriesTableProperty creditColumnManager;
	
	ShoeboxFormPage formPage;
	
	/**
	 * The transaction manager used for all changes made by
	 * this page.  It is created by the page is created and
	 * remains usable for the rest of the time that this page
	 * exists.
	 */
	TransactionManager transactionManager = null;

	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
        // Create our own transaction manager.
        // This ensures that uncommitted changes
    	// made by this page are isolated from datastore usage outside
    	// of this page.
		DatastoreManager sessionManager = JMoneyPlugin.getDefault().getSessionManager();

        transactionManager = new TransactionManager(sessionManager);
    	
    	// Build an array of all possible properties that may be
    	// displayed in the table.
       
        // TODO: I think this following line needs to be in the other pages too.
        // (account entries and reconciliation).  We otherwise seem to get the
        // columns added multiple times.
    	allEntryDataObjects = new Vector<IEntriesTableProperty>();
        
        // Add properties from the transaction.
        for (Iterator iter = TransactionInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            final PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor.isScalar()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "transaction") {
					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
						return data.getTransactionForTransactionFields();
					}
            	});
            }
        }

        /*
		 * Add properties from this entry. For time being, this is all the
		 * properties except the description which come from the other entry,
		 * and the amount which is shown in the debit and credit columns.
		 */
        for (Iterator iter = EntryInfo.getPropertySet().getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor != EntryInfo.getDescriptionAccessor()
        		&& propertyAccessor != EntryInfo.getAmountAccessor()) {
            	if (propertyAccessor.isScalar() && propertyAccessor.isEditable()) {
            		allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "this") {
    					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
    						return data.getEntryForAccountFields();
    					}
                	});
            	}
            }
        }

        /*
		 * Add properties from the other entry where the property also is
		 * applicable for capital accounts. For time being, this is just the
		 * account.
		 */
        PropertySet extendablePropertySet = EntryInfo.getPropertySet();
        for (Iterator iter = extendablePropertySet.getPropertyIterator3(); iter.hasNext();) {
            PropertyAccessor propertyAccessor = (PropertyAccessor) iter.next();
            if (propertyAccessor == EntryInfo.getAccountAccessor()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "common2") {
					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
						return data.getEntryForCommon2Fields();
					}
            	});
            } else if (propertyAccessor == EntryInfo.getDescriptionAccessor()) {
            	allEntryDataObjects.add(new EntriesSectionProperty(propertyAccessor, "other") {
					public ExtendableObject getObjectContainingProperty(IDisplayableItem data) {
						return data.getEntryForOtherFields();
					}
            	});
            }
        }
        
		debitColumnManager = new DebitAndCreditColumns("Debit", "debit", true);     //$NON-NLS-2$
		creditColumnManager = new DebitAndCreditColumns("Credit", "credit", false); //$NON-NLS-2$

    	
		formPage = new ShoeboxFormPage(
				editor,
				PAGE_ID);
	
		try {
			editor.addPage(formPage);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
			// TODO: cleanly leave out this page.
		}
		
		return formPage;
	}

	private class ShoeboxFormPage extends SectionlessPage {
		
		private Session session;
		
	    private EntriesTree recentlyAddedEntriesControl;
	    private IEntriesContent recentEntriesTableContents = null;
	    
		Collection<IObjectKey> ourEntryList = new Vector<IObjectKey>();
		
		public Map<String, ITransactionTemplate> transactionTypes = new HashMap<String, ITransactionTemplate>();

		ShoeboxFormPage(
				NodeEditor editor,
				String pageId) {
			super(editor,
					pageId, 
					ShoeboxPlugin.getResourceString("navigationTreeLabel"), 
			"Shoebox Receipts - data entry");
			
			this.session = JMoneyPlugin.getDefault().getSession();
		}
		
		public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
			
	        recentEntriesTableContents = new IEntriesContent() {

				public Vector getAllEntryDataObjects() {
					return allEntryDataObjects;
				}

				public IEntriesTableProperty getDebitColumnManager() {
					return debitColumnManager;
				}

				public IEntriesTableProperty getCreditColumnManager() {
					return creditColumnManager;
				}

				public IEntriesTableProperty getBalanceColumnManager() {
					// But there is no balance column?????
					return null;
				}

				public Collection<Entry> getEntries() {
					Collection<Entry> committedEntries = new Vector<Entry>();
					for (IObjectKey objectKey: ourEntryList) {
						Entry committedEntry = (Entry)((UncommittedObjectKey)objectKey).getCommittedObjectKey().getObject();
						committedEntries.add(committedEntry);
					}
					return committedEntries;
				}

				public boolean isEntryInTable(Entry entry) {
					/*
					 * This entry is to be shown if the entry was entered using
					 * this editor. We keep a list of entries that were entered
					 * through this editor.
					 */
					for (IObjectKey objectKey: ourEntryList) {
						IObjectKey committedKey = ((UncommittedObjectKey)objectKey).getCommittedObjectKey();
						if (committedKey.equals(entry.getObjectKey())) {
							return true;
						}
					}
					return false;
				}

				public boolean isEntryInTable(Entry entry, PropertyAccessor propertyAccessor, Object value) {
					/*
					 * Property changes don't affect whether the entry should
					 * be in the table, so just see if in our list.
					 */
					return isEntryInTable(entry);
				}

				public boolean filterEntry(IDisplayableItem data) {
					// No filter here, so entries always match
					return true;
				}

				public long getStartBalance() {
					// No balance in this table
					return 0;
				}

				public void setNewEntryProperties(Entry newEntry) {
					/*
					 * There are no properties we must set when an entry is
					 * added to this table.
					 */
				}
	        };

			/**
			 * topLevelControl is a control with grid layout, 
			 * with one column of vertical controls.
			 */
			Composite topLevelControl = new Composite(parent, SWT.NULL);
			topLevelControl.setLayout(new GridLayout(1, false));
			parent.setBackground(this.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
			topLevelControl.setBackground(getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_CYAN));
			
	        // Create the table control.
	        recentlyAddedEntriesControl = new EntriesTree(topLevelControl, toolkit, transactionManager, recentEntriesTableContents, this.session); 
			
			recentlyAddedEntriesControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

			// The tab control
			GridData gdTabControl = new GridData(SWT.FILL, SWT.FILL, false, false);
			createTabbedArea(topLevelControl).setLayoutData(gdTabControl);

			// init from the memento
			init(memento);
			
			// Set up the context menus.
			makeActions();
			hookContextMenu(fEditor.getSite());
			
			return topLevelControl;
		}
		
		private Control createTabbedArea(Composite parent) {
			TabFolder tabFolder = new TabFolder(parent, SWT.NONE);

			
			// Load the extensions
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.shoebox.templates");
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				IConfigurationElement[] elements =
					extensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals("template")) {
						
						String label = elements[j].getAttribute("label");
						String id = elements[j].getAttribute("id");
						String position = elements[j].getAttribute("position");
						String fullId = extensions[i].getUniqueIdentifier() + "." + id;
						
						try {
							ITransactionTemplate transactionType = (ITransactionTemplate)elements[j].createExecutableExtension("class");
							
				    		TabItem tabItem = new TabItem(tabFolder, SWT.NULL);
				    		tabItem.setText(transactionType.getDescription());
				    		tabItem.setControl(transactionType.createControl(tabFolder, true, null, ourEntryList));
								
				    		int positionNumber = 800;
				    		if (position != null) {
				    			positionNumber = Integer.parseInt(position);
				    		}
		
							transactionTypes.put(fullId, transactionType);
							
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			
	        return tabFolder;
		}

		private void init(IMemento memento) {
			if (memento != null) {
				IMemento [] templateMementos = memento.getChildren("template");
				for (int i = 0; i < templateMementos.length; i++) {
					ITransactionTemplate transactionType = (ITransactionTemplate)transactionTypes.get(templateMementos[i].getID());
					if (transactionType != null) {
						transactionType.init(templateMementos[i]);
					}
				}
			}
		}
		
		public void saveState(IMemento memento) {
			for (Iterator iter = transactionTypes.keySet().iterator(); iter.hasNext(); ) {
				String id = (String)iter.next();
				ITransactionTemplate transactionType = (ITransactionTemplate)transactionTypes.get(id);
				transactionType.saveState(memento.createChild("template", id));
			}
		}
		
		private void hookContextMenu(IWorkbenchPartSite site) {
		}
		
        private void makeActions() {
		}
		
	};
	
	/**
	 * Represents a property that can be displayed in the entries table,
	 * edited by the user, or used in the filter.
	 * <P>
	 * The credit, debit, and balance columns are hard coded at the end
	 * of the table and are not represented by objects of this class.
	 * 
	 * @author Nigel Westbury
	 */
	abstract class EntriesSectionProperty implements IEntriesTableProperty {
		private PropertyAccessor accessor;
		private String id;
		
		EntriesSectionProperty(PropertyAccessor accessor, String source) {
			this.accessor = accessor;
			this.id = source + '.' + accessor.getName();
		}

		public String getText() {
			return accessor.getShortDescription();
		}

		public String getId() {
			return id;
		}

		public int getWeight() {
			return accessor.getWeight();
		}

		public int getMinimumWidth() {
			return accessor.getMinimumWidth();
		}

		/**
		 * @param entry
		 * @return
		 */
		public String getValueFormattedForTable(IDisplayableItem data) {
			ExtendableObject extendableObject = getObjectContainingProperty(data);
			if (extendableObject == null) {
				return "";
			} else {
				return accessor.formatValueForTable(extendableObject);
			}
		}

		public abstract ExtendableObject getObjectContainingProperty(IDisplayableItem data);

		/**
		 * @param table
		 * @return
		 */
		public IPropertyControl createAndLoadPropertyControl(Composite parent, IDisplayableItem data) {
			Session session = data.getTransaction().getSession();
			
			IPropertyControl propertyControl = accessor.createPropertyControl(parent, session); 
				
			ExtendableObject extendableObject = getObjectContainingProperty(data);

			/*
			 * If the returned object is null, that means this column contains a
			 * property that does not apply to this row. Perhaps the property is
			 * the transaction date and this is a split entry, or perhaps the
			 * property is a property for an income and expense category but
			 * this row is a transfer transaction. We return null to indicate
			 * that the cell is not editable.
			 */
			if (extendableObject == null) {
					return null;
			}
				
			propertyControl.load(extendableObject);
			
			return propertyControl;
		}

		/**
		 * @return
		 */
		public boolean isTransactionProperty() {
			return accessor.getExtendablePropertySet() == TransactionInfo.getPropertySet();
		}

		public int compare(DisplayableTransaction trans1, DisplayableTransaction trans2) {
			ExtendableObject extendableObject1 = getObjectContainingProperty(trans1);
			ExtendableObject extendableObject2 = getObjectContainingProperty(trans2);

			int result;
			
			/*
			 * First deal with null cases. If no object contains this property
			 * on this row then the cell is blank.
			 * 
			 * Null values are sorted first. It is necessary to put null values
			 * first because empty strings are sorted first, and users may not
			 * be aware of the difference.
			 */
			if (extendableObject1 == null && extendableObject2 == null) {
				return 0;
			} else if (extendableObject1 == null) {
				return -1;
			} else if (extendableObject2 == null) {
				return 1;
			}
				
			Object value1 = extendableObject1.getPropertyValue(accessor);
			Object value2 = extendableObject2.getPropertyValue(accessor);
			
			if (accessor.getCustomComparator() != null) { 
				result = accessor.getCustomComparator().compare(value1, value2);
			} else {
				if (accessor.getValueClass() == Date.class) {
					result = ((Date)value1).compareTo((Date)value2);
				} else if (accessor.getValueClass() == Integer.class) {
					result = ((Integer)value1).compareTo((Integer)value2);
				} else if (accessor.getValueClass() == Long.class) {
					result = ((Long)value1).compareTo((Long)value2);
				} else {
					/*
					 * No custom comparator and not a known type, so sort
					 * according to the text value that is displayed when the
					 * property is shown in a table (ignoring case).
					 */
					String text1 = accessor.formatValueForTable(extendableObject1);
					String text2 = accessor.formatValueForTable(extendableObject2);
					result = text1.compareToIgnoreCase(text2);
				}
			}
			
			return result;
		}
	}

	
	/**
	 * Represents a table column that is either the debit or the credit column.
	 * Use two instances of this class instead of a single instance of the
	 * above <code>EntriesSectionProperty</code> class if you want the amount to be
	 * displayed in seperate debit and credit columns.
	 */
	class DebitAndCreditColumns implements IEntriesTableProperty {
		private String id;
		private String name;
		private boolean isDebit;
		
		DebitAndCreditColumns(String id, String name, boolean isDebit) {
			this.id = id;
			this.name = name;
			this.isDebit = isDebit;
		}
		
		public String getText() {
			return name;
		}

		public String getId() {
			return id;
		}

		public int getWeight() {
			return 2;
		}

		public int getMinimumWidth() {
			return 70;
		}

		public String getValueFormattedForTable(IDisplayableItem data) {
			Entry entry = data.getEntryForThisRow();
			if (entry == null) {
				return "";
			}
			
			long amount = entry.getAmount();

			Commodity commodity = entry.getCommodity();
			if (commodity == null) {
				/*
				 * The commodity should never be null after all the data for the
				 * entry has been entered. However, the user may enter an amount
				 * before entering the currency, and the best we can do in such
				 * a situation is to format the amount assuming the default
				 * currency the account.
				 */
				commodity = entry.getSession().getDefaultCurrency();
			}

			if (isDebit) {
				return amount < 0 ? commodity.format(-amount) : "";
			} else {
				return amount > 0 ? commodity.format(amount) : "";
			}
		}

		public IPropertyControl createAndLoadPropertyControl(Composite parent, IDisplayableItem data) {
			// This is the entry whose amount is being edited by
			// this control.
			final Entry entry = data.getEntryForThisRow();
			if (entry == null) {
				return null;
			}
			
			long amount = entry.getAmount();

			final Text textControl = new Text(parent, SWT.NONE);

			Commodity commodity = entry.getSession().getDefaultCurrency();
			if (isDebit) {
				// Debit column
				textControl.setText(amount < 0 
						? commodity.format(-amount) 
								: ""
				);
			} else {
				// Credit column
				textControl.setText(amount > 0 
						? commodity.format(amount) 
								: ""
				);
			}

			IPropertyControl propertyControl = new IPropertyControl() {
				public Control getControl() {
					return textControl;
				}
				public void load(ExtendableObject object) {
					throw new RuntimeException();
				}
				public void save() {
					// We need a currency so that we can format the amount.
					// Get the currency from this entry if possible.
					// However, the user may not have yet entered enough information
					// to determine the currency for this entry, in which case
					// use the currency for the account being listed in this editor.
					// FIXME change this when we can get the currency for income/expense
					// accounts.
					Commodity commodityForFormatting = null;
					if (entry.getAccount() != null
							&& entry.getAccount() instanceof CapitalAccount) {
						commodityForFormatting = entry.getCommodity();
					}
					if (commodityForFormatting == null) {
						commodityForFormatting = entry.getSession().getDefaultCurrency();
					}
					
					String amountString = textControl.getText();
					long amount = commodityForFormatting.parse(amountString);
					
					long previousEntryAmount = entry.getAmount();
					long newEntryAmount;
					
					if (isDebit) {
						if (amount != 0) {
							newEntryAmount = -amount;
						} else {
							if (previousEntryAmount < 0) { 
								newEntryAmount  = 0;
							} else {
								newEntryAmount = previousEntryAmount;
							}
						}
					} else {
						if (amount != 0) {
							newEntryAmount = amount;
						} else {
							if (previousEntryAmount > 0) { 
								newEntryAmount  = 0;
							} else {
								newEntryAmount = previousEntryAmount;
							}
						}
					}

					entry.setAmount(newEntryAmount);

					/*
					 * If there are two entries in the transaction and if both
					 * entries have accounts in the same currency or one or
					 * other account is not known or one or other account is a
					 * multi-currency account then we set the amount in the
					 * other entry to be the same but opposite signed amount.
					 */
					if (entry.getTransaction().hasTwoEntries()) {
						Entry otherEntry = entry.getTransaction().getOther(entry);
						Commodity commodity1 = entry.getCommodity();
						Commodity commodity2 = otherEntry.getCommodity();
						if (commodity1 == null || commodity2 == null || commodity1.equals(commodity2)) {
							otherEntry.setAmount(-newEntryAmount);
						}
					}
				}
			};
			
			return propertyControl;
		}

		public boolean isTransactionProperty() {
			return false;
		}

		public int compare(DisplayableTransaction trans1, DisplayableTransaction trans2) {
			long amount1 = trans1.getEntryForThisRow().getAmount();
			long amount2 = trans2.getEntryForThisRow().getAmount();
			
			int result;
			if (amount1 < amount2) {
				result = -1;
			} else if (amount1 > amount2) {
				result = 1;
			} else {
				result = 0;
			}

			// If debit column then reverse.  Ascending sort should
			// result in the user seeing ascending numbers in the
			// sorted column.
			if (isDebit) {
				result = -result;
			}
			
			return result;
		}
    }
}
