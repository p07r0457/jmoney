/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.views;

import java.awt.FlowLayout;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.part.*;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.SWT;

import net.sf.jmoney.IBookkeepingPageListener;
import net.sf.jmoney.model2.Session;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to te
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class FolderView extends ViewPart {
    public static final String ID_VIEW = "net.sf.jmoney.views.FolderView"; //$NON-NLS-1$

    /**
     * Top level control in this view. This may be a plug-in supplied control,
     * or it may be a TabFolder if there was more than one plug-in supplied
     * control.
     */
    private Control singleItemViewer = null;

    private TabFolder tabFolderViewer = null;

    private Composite parent;

    //The shared instance.
    // TODO: remove this?
    private static FolderView view;

    public FolderView() {
        view = this;
    }

    public static FolderView getDefault() {
        return view;
    }

    public void createPartControl(Composite parent) {
        this.parent = parent;

        parent.getParent().setBackground(
                parent.getDisplay().getSystemColor(SWT.COLOR_CYAN));
        parent.getParent().setLayout(new FillLayout());
        parent.getParent().pack(false);

        // Modified by Faucheux
        // The one and only visible control in the parent
        // should fill the entire space.
        parent.setBackground(parent.getDisplay().getSystemColor(
                SWT.COLOR_DARK_BLUE));
        parent.pack(false);

        // Force the parent (DARK_BLUE) to be as big as its container
        parent.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) { /* nothing */ }
            public void controlResized(ControlEvent e) {
                Composite parent = (Composite) e.getSource();
                System.out.println("Redraw parent " + parent
                        + " from 'FolderView.java'");
                System.out.println("  was " + parent.getSize());
                parent.setSize(parent.getParent().getSize());
                System.out.println("  is  " + parent.getSize());
            }
            
        });

    }

    /**
     * The navigation view calls this method to indicate that a selection has
     * been made in the navigation view.
     * 
     * @param pageListeners
     *            An array of objects that implement IBookkeepingPageListener.
     *            This parameter is never null but may be empty if there are no
     *            page listeners.
     */
    public void setSelectedObject(Vector pageListeners, Object selectedObject,
            Session session) {
        if (singleItemViewer != null) {
            singleItemViewer.dispose();
            singleItemViewer = null;
        }

        // Dispose of all the items in the tab folder.
        // The array returned by getItems is a copy so can
        // be safely iterated while we dispose the items.
        if (tabFolderViewer != null) {
        	TabItem items[] = tabFolderViewer.getItems();
        	for (int i = 0; i < items.length; i++) {
        		items[i].dispose();
        	}
        }

        if (selectedObject != null) {
            // First count the pages. If there are no pages then
            // we leave the view empty, if there is one page then
            // that page is placed directly in this view as the sole
            // control in this view. If there is more than one
            // page then we create a TabFolder.
            int pageCount = 0;
            for (Iterator iter = pageListeners.iterator(); iter.hasNext();) {
                IBookkeepingPageListener pageListener = (IBookkeepingPageListener) iter
                        .next();
                pageCount += pageListener.getPageCount(selectedObject);
            }

            if (pageCount == 1) {
            	if (tabFolderViewer != null) {
            		// unfortunately we need to completely dispose the tab viewer.
            		tabFolderViewer.dispose();
            		tabFolderViewer = null;
            	}
            	
                for (Iterator iter = pageListeners.iterator(); iter.hasNext();) {
                    IBookkeepingPageListener pageListener = (IBookkeepingPageListener) iter
                            .next();
                    if (pageListener.getPageCount(selectedObject) > 0) {
                        IBookkeepingPageListener.BookkeepingPage pages[] = pageListener
                                .createPages(selectedObject, session, parent);
                        // If the plug-in says it only creates one page
                        // but in fact creates zero pages then we have a
                        // plug-in error. We simply leave the viewer null.
                        if (pages.length > 0) {
                            singleItemViewer = pages[0].getControl();
                            // If the plug-in produced more than one view
                            // then discard the rest now.
                            for (int i = 1; i < pages.length; i++) {
                                if (pages[0].getControl() != null) {
                                    pages[0].getControl().dispose();
                                }
                            }
                        }
                        // TODO Handle this plug-in error better.
                        break;
                    }
                }
            } else if (pageCount > 1) {
            	if (tabFolderViewer == null) {
                    tabFolderViewer = new TabFolder(parent, SWT.TOP);
                    tabFolderViewer.setBackground(tabFolderViewer.getDisplay()
                            .getSystemColor(SWT.COLOR_GREEN));

                    // Force the parent (DARK_BLUE) to be as big as its container
                    tabFolderViewer.getParent().addControlListener(new ControlListener() {
                        public void controlMoved(ControlEvent e) { /* nothing */ }
                        public void controlResized(ControlEvent e) {
                            Composite parent = (Composite) e.getSource();
                            System.out.println("Redraw parent " + parent
                                    + " from 'FolderView.java'");
                            System.out.println("  was " + parent.getSize());
                            parent.setSize(parent.getParent().getSize());
                            System.out.println("  is  " + parent.getSize());
                        }
                        
                    });
            	}
            	

                for (Iterator iter = pageListeners.iterator(); iter.hasNext();) {
                    IBookkeepingPageListener pageListener = (IBookkeepingPageListener) iter
                            .next();
                    if (pageListener.getPageCount(selectedObject) > 0) {
                        IBookkeepingPageListener.BookkeepingPage pages[] = pageListener
                                .createPages(selectedObject, session,
                                        tabFolderViewer);
                        for (int i = 0; i < pages.length; i++) {
                            TabItem tabItem = new TabItem(tabFolderViewer, 0);
                            tabItem.setText(pages[i].getText());
                            tabItem.setControl(pages[i].getControl());
                        }
                    }
                }
            }
        }

        // If 'pack' is not called then the swing control often does not
        // display.
        // TODO: understand this.
        parent.pack();
    }

    public void setFocus() {
        if (singleItemViewer != null) {
            singleItemViewer.setFocus();
        } else if (tabFolderViewer != null) {
            tabFolderViewer.setFocus();
        }
    }
}