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

package net.sf.jmoney;

import net.sf.jmoney.views.FolderView;
import net.sf.jmoney.views.NavigationView;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * @author Nigel
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class JMoneyPerspective implements IPerspectiveFactory {

	    public JMoneyPerspective() {
	    }

	    public void createInitialLayout(IPageLayout layout) {
	        // TODO: Figure out why this line has no effect (as of M8)
	        layout.setFixed(true);

	        layout.setEditorAreaVisible(false);

	        layout.addView(
		        NavigationView.ID_VIEW,
	            IPageLayout.TOP,
	            IPageLayout.RATIO_MAX,
	            IPageLayout.ID_EDITOR_AREA);
	        layout.addPerspectiveShortcut(NavigationView.ID_VIEW);
	        layout.addShowViewShortcut(NavigationView.ID_VIEW);

	        layout.addView(
	        	FolderView.ID_VIEW,
	            IPageLayout.RIGHT,
	            0.20f,
				NavigationView.ID_VIEW);
	        layout.addPerspectiveShortcut("net.sf.jmoney.JMoneyPerspective");
	        layout.addShowViewShortcut(FolderView.ID_VIEW);
	    }
	}
