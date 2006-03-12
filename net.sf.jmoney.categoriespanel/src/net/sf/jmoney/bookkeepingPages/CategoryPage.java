/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
 *  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.bookkeepingPages;

import java.util.Iterator;
import java.util.Vector;

import net.sf.jmoney.Constants;
import net.sf.jmoney.IBookkeepingPage;
import net.sf.jmoney.IBookkeepingPageFactory;
import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.categoriespanel.CategoriesPanelPlugin;
import net.sf.jmoney.fields.AccountInfo;
import net.sf.jmoney.fields.IncomeExpenseAccountInfo;
import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IPropertyDependency;
import net.sf.jmoney.model2.IncomeExpenseAccount;
import net.sf.jmoney.model2.PropertyAccessor;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.SessionChangeAdapter;
import net.sf.jmoney.model2.SessionChangeListener;
import net.sf.jmoney.views.NodeEditor;
import net.sf.jmoney.views.SectionlessPage;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author Nigel Westbury
 * @author Johann Gyger
 */
public class CategoryPage implements IBookkeepingPageFactory {
	
	private static final String PAGE_ID = "net.sf.jmoney.categoriespanel.categories";
	
	
	public void init(IMemento memento) {
		// No view state to restore
	}
	
	public void saveState(IMemento memento) {
		// No view state to save
	}
	
	/* (non-Javadoc)
	 * @see net.sf.jmoney.IBookkeepingPageListener#createPages(java.lang.Object, org.eclipse.swt.widgets.Composite)
	 */
	public IBookkeepingPage createFormPage(NodeEditor editor, IMemento memento) {
		SectionlessPage formPage = new CategoryFormPage(editor, PAGE_ID);
		
		try {
			editor.addPage(formPage);
		} catch (PartInitException e) {
			JMoneyPlugin.log(e);
			// TODO: cleanly leave out this page.
		}
		
		return formPage;
	}
	
	private class CategoryFormPage extends SectionlessPage {
		private TreeViewer viewer;
		//	private DrillDownAdapter drillDownAdapter;
		private Action newAccountAction;
		private Action newSubAccountAction;
		private Action deleteAccountAction;
		private Action editorAction;
		
		/**
		 * The account whose property values are in the edit controls below.
		 * If selectedAccount is null then the property controls should
		 * should be disabled.
		 */
		private IncomeExpenseAccount selectedAccount = null;
		
		private Session session;
		
		private class PropertyControls {
			
			private PropertyAccessor propertyAccessor;
			private Label propertyLabel;
			private IPropertyControl propertyControl;
			
			PropertyControls(PropertyAccessor propertyAccessor, 
					Label propertyLabel,
					IPropertyControl propertyControl) {
				this.propertyAccessor = propertyAccessor;
				this.propertyLabel = propertyLabel;
				this.propertyControl = propertyControl;
			}
			
			void load(ExtendableObject object) {
				IPropertyDependency dependency = propertyAccessor.getDependency();
				if (dependency != null) {
					boolean isApplicable = dependency.isSelected(selectedAccount);
					if (isApplicable) {
						propertyControl.load(object);
					}
					propertyLabel.setVisible(isApplicable);
					propertyControl.getControl().setVisible(isApplicable);
				} else {
					propertyControl.load(object);
				}
			}
			
			/**
			 * Called whenever a property changes.
			 * The visability of all property controls with dependencies are updated. 
			 */
			public void setVisibility() {
				IPropertyDependency dependency = propertyAccessor.getDependency();
				if (dependency != null) {
					boolean isApplicable = dependency.isSelected(selectedAccount);
					propertyLabel.setVisible(isApplicable);
					propertyControl.getControl().setVisible(isApplicable);
					
					// We don't load controls if the property does not apply.
					// That is probably not the correct thing to do, but until
					// it is designed better, that means we must load now.
					if (isApplicable) {
						propertyControl.load(selectedAccount);
					}
				}
			}
		}
		
		/**
		 * List of the PropertyControls objects for the
		 * properties that can be edited in this panel.
		 * element: PropertyControls
		 */
		Vector propertyList = new Vector();
		
		private SessionChangeListener listener = new SessionChangeAdapter() {
			public void sessionReplaced(Session oldSession, Session newSession) {
				// When the session is replaced, this view will be disposed
				// and possibly re-built, so do nothing here.
			}
			
			public void objectAdded(ExtendableObject newObject) {
				if (newObject instanceof IncomeExpenseAccount) {
					IncomeExpenseAccount newAccount = (IncomeExpenseAccount)newObject;
					Account parent = newAccount.getParent();
					if (parent == null) {
						viewer.refresh(session, false);
					} else {
						viewer.refresh(parent, false);
					}
				}
			}
			
			public void objectDeleted(ExtendableObject deletedObject) {
                if (deletedObject instanceof IncomeExpenseAccount) {
                    IncomeExpenseAccount deletedAccount = (IncomeExpenseAccount) deletedObject;
                    viewer.setSelection(null);
                    viewer.remove(deletedAccount);
                }
            }
			
			public void objectChanged(ExtendableObject changedObject, PropertyAccessor propertyAccessor, Object oldValue, Object newValue) {
				if (changedObject instanceof IncomeExpenseAccount) {
					IncomeExpenseAccount account = (IncomeExpenseAccount)changedObject;
					if (propertyAccessor == AccountInfo.getNameAccessor()) {
						Account parent = account.getParent();
						// We refresh the parent node because the name change
						// in this node may affect the order of the child nodes.
						if (parent == null) {
							viewer.refresh(session, true);
						} else {
							viewer.refresh(parent, true);
						}
					}
					
					if (account.equals(selectedAccount)) {
						// Update the visibility of controls.
						for (Iterator iter = propertyList.iterator(); iter.hasNext(); ) {
							PropertyControls propertyControls = (PropertyControls)iter.next();
							propertyControls.setVisibility();
						}
					}
				}
			}
		};
		
		CategoryFormPage(NodeEditor editor, String pageId) {
            super(editor, pageId, CategoriesPanelPlugin
                    .getResourceString("NavigationTreeModel.categories"),
                    "Income and Expense Categories");

            this.session = JMoneyPlugin.getDefault().getSession();
        }
		
		public Composite createControl(Object nodeObject, Composite parent, FormToolkit toolkit, IMemento memento) {
			
			/**
			 * topLevelControl is a control with grid layout, 
			 * onto which all sub-controls should be placed.
			 */
			Composite topLevelControl = new Composite(parent, SWT.NULL);
			
			GridLayout layout = new GridLayout();
			layout.numColumns = 2;
			topLevelControl.setLayout(layout);
			
			viewer = new TreeViewer(topLevelControl, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
			
			GridData gridData = new GridData();
			gridData.horizontalAlignment = GridData.FILL;
			gridData.verticalAlignment = GridData.FILL;
			gridData.grabExcessHorizontalSpace = true;
			gridData.grabExcessVerticalSpace = true;
			gridData.horizontalSpan = 2;
			viewer.getControl().setLayoutData(gridData);
			
			//		drillDownAdapter = new DrillDownAdapter(viewer);
			ViewContentProvider contentProvider = new ViewContentProvider();
			viewer.setContentProvider(contentProvider);
			viewer.setLabelProvider(new ViewLabelProvider());
			viewer.setSorter(new NameSorter());
			
			viewer.setInput(session);

			// Listen for changes to the category list.
			JMoneyPlugin.getDefault().addSessionChangeListener(listener, viewer.getControl());
			
			// Listen for changes in the selection and update the 
			// edit controls.
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					// If a selection already selected, commit any changes
					if (selectedAccount != null) {
						session.registerUndoableChange("change category properties");
					}
					
					// Set the new selection
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    selectedAccount = (IncomeExpenseAccount) selection.getFirstElement();
					
					// Set the values from the account object into the control fields,
					// or disable the controls if the account is null.
					for (Iterator iter = propertyList.iterator(); iter.hasNext(); ) {
						PropertyControls propertyControls = (PropertyControls)iter.next();
						propertyControls.load(selectedAccount);
					}
				}
			});
			
			// Add the properties for category.
			PropertySet extendablePropertySet = IncomeExpenseAccountInfo.getPropertySet();
			for (Iterator iter = extendablePropertySet.getPropertyIterator3(); iter.hasNext(); ) {
				final PropertyAccessor propertyAccessor = (PropertyAccessor)iter.next();
				if (propertyAccessor.isScalar()) {
					final Label propertyLabel = new Label(topLevelControl, 0);
					propertyLabel.setText(propertyAccessor.getShortDescription() + ':');
					final IPropertyControl propertyControl = propertyAccessor.createPropertyControl(topLevelControl);
					propertyControl.getControl().addFocusListener(
							new FocusAdapter() {
								
								// When a control gets the focus, save the old value here.
								// This value is used in the change message.
								String oldValueText;
								
								public void focusLost(FocusEvent e) {
									if (session.isSessionFiring()) {
										return;
									}
									
									propertyControl.save();
									String newValueText = propertyAccessor.formatValueForMessage(
											selectedAccount);
									
									String description;
									if (propertyAccessor == AccountInfo.getNameAccessor()) {
										description = 
											"rename account from " + oldValueText
											+ " to " + newValueText;
									} else {
										description = 
											"change " + propertyAccessor.getShortDescription() + " property"
											+ " in '" + selectedAccount.getName() + "' account"
											+ " from " + oldValueText
											+ " to " + newValueText;
									}
									session.registerUndoableChange(description);
								}
								public void focusGained(FocusEvent e) {
									// Save the old value of this property for use in our 'undo' message.
									oldValueText = propertyAccessor.formatValueForMessage(
											selectedAccount);
								}
							});
					
					// No account is initially set.  It is not really
					// obvious in what state the controls should be when no
					// account is set, so let's leave any that could be
					// inapplicable as invisible, and the others visible
					// but disabled.
					if (propertyAccessor.getDependency() != null) {
						propertyLabel.setVisible(false);
						propertyControl.getControl().setVisible(false);
					}
					
					// Add to our list of controls.
					propertyList.add(
							new PropertyControls(propertyAccessor, propertyLabel, propertyControl));
					
					toolkit.adapt(propertyLabel, false, false);
					toolkit.adapt(propertyControl.getControl(), true, true);
					
					// Make the control take up the full width
					GridData gridData5 = new GridData();
					gridData.horizontalAlignment = GridData.FILL;
					gridData5.grabExcessHorizontalSpace = true;
					propertyControl.getControl().setLayoutData(gridData5);
					
					// Set the control to have no account set (control
					// is disabled)
					propertyControl.load(null);
				}
			}
			
			
			// Set up the context menus.
			makeActions();
			hookContextMenu(fEditor.getSite());
			
			return topLevelControl;
		}
		
		public void saveState(IMemento memento) {
			// We could save the current category selection
			// and the expand/collapse state of each node
			// but it is not worthwhile.
		}

		private void hookContextMenu(IWorkbenchPartSite site) {
			MenuManager menuMgr = new MenuManager("#PopupMenu");
			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(IMenuManager manager) {
					CategoryFormPage.this.fillContextMenu(manager);
				}
			});
			Menu menu = menuMgr.createContextMenu(viewer.getControl());
			viewer.getControl().setMenu(menu);
			
			site.registerContextMenu(menuMgr, viewer);
		}
		
        private void fillContextMenu(IMenuManager manager) {
            manager.add(newAccountAction);
            if (selectedAccount != null) {
                manager.add(newSubAccountAction);
                manager.add(deleteAccountAction);
            }

            manager.add(new Separator());

            // Add a menu item for IncomeExpenseAccount editor
            if (selectedAccount != null && editorAction != null) {
                manager.add(editorAction);
            }

            manager.add(new Separator());

            // Other plug-ins can contribute their actions here
            manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
        }
		
        private void makeActions() {
			newAccountAction = new Action() {
                public void run() {
                    IncomeExpenseAccount account = (IncomeExpenseAccount) session.createAccount(IncomeExpenseAccountInfo
                            .getPropertySet());
                    account.setName(CategoriesPanelPlugin.getResourceString("CategoryPanel.newCategory"));
                    session.registerUndoableChange("add new category");
                    viewer.setSelection(new StructuredSelection(account), true);
                }
            };
            newAccountAction.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.newCategory"));
            newAccountAction.setToolTipText("New category tooltip");

            newSubAccountAction = new Action() {
                public void run() {
                    if (selectedAccount != null) {
                        IncomeExpenseAccount subAccount = selectedAccount.createSubAccount();
                        subAccount.setName(CategoriesPanelPlugin.getResourceString("CategoryPanel.newCategory"));
                        session.registerUndoableChange("add new category");
                        viewer.setSelection(new StructuredSelection(subAccount), true);
                    }
                }
            };
			newSubAccountAction.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.newSubcategory"));
			newSubAccountAction.setToolTipText("New category tooltip");
			
			deleteAccountAction = new Action() {
				public void run() {
					if (selectedAccount != null) {
						session.deleteAccount(selectedAccount);
					}
				}
			};
			deleteAccountAction.setText(CategoriesPanelPlugin.getResourceString("CategoryPanel.deleteCategory"));
			deleteAccountAction.setToolTipText("Delete category tooltip");

			if (!IncomeExpenseAccountInfo.getPropertySet().getPageFactories().isEmpty()) {
				editorAction = new Action() {
					public void run() {
						IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
						for (Iterator iterator = selection.iterator(); iterator.hasNext();) {
							Object selectedObject = iterator.next();
							JMoneyPlugin.myAssert(selectedObject instanceof ExtendableObject); 
							NodeEditor.openEditor(
									getSite().getWorkbenchWindow(),
									(ExtendableObject)selectedObject);
						}
					}
				};
				editorAction.setText(JMoneyPlugin.getResourceString("Menu.openCategoryAccountEditor"));
			} else {
				// No plug-ins have added any pages that display category information,
				// so do not show a menu item for this.
				editorAction = null;
			}
		}
		
	};
	
	class ViewContentProvider implements IStructuredContentProvider, 
	ITreeContentProvider {
		/**
		 * In fact the input does not change because we create our own node object
		 * that acts as the root node.  Certain nodes below the root may get
		 * their data from the model.  The accountsNode object does this.
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			//			The input never changes so we don't do anything here.
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof Account) {
				Account parent = ((Account)child).getParent();
				if (parent == null) {
					return ((Account)child).getSession();
				} else {
					return parent;
				}
			}
			return null;
		}
		
		public Object [] getChildren(Object parent) {
			// TODO: The nodes are not currently ordered, but they
			// should be.  
			
			if (parent instanceof Session) {
				Iterator iter;

				iter = ((Session)parent).getIncomeExpenseAccountIterator();
				int count = 0;
				for ( ; iter.hasNext(); ) {
					iter.next();
					count++;
				}
				Object children[] = new Object[count];
				iter = ((Session)parent).getIncomeExpenseAccountIterator();
				int i = 0;
				for ( ; iter.hasNext(); ) {
					children[i++] = iter.next();
				}
				return children;
			} else if (parent instanceof IncomeExpenseAccount) {
				return ((IncomeExpenseAccount)parent).getSubAccountCollection().toArray();
			} else {
				throw new RuntimeException("internal error");
			}
		}
		
		public boolean hasChildren(Object parent) {
			if (parent instanceof Session) {
				return ((Session)parent).getIncomeExpenseAccountIterator().hasNext();
			} else if (parent instanceof Account) {
				return !((Account)parent).getSubAccountCollection().isEmpty();
			}
			return false;
		}
		
	}
	
	class ViewLabelProvider extends LabelProvider {

        public String getText(Object obj) {
            if (obj instanceof Account) {
                String name = ((Account) obj).getName();
                return name == null? "(unknown account name)": name;
            } else {
                return "(unknown object)";
            }
        }

        public Image getImage(Object obj) {
            if (obj instanceof Account) {
                return Constants.CATEGORY_ICON;
            } else {
                throw new RuntimeException("");
            }
        }

    }
	
	class NameSorter extends ViewerSorter {
	}
	
}
