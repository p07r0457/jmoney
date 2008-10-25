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

package net.sf.jmoney.stocks.views;

import java.util.ArrayList;
import java.util.Collection;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.Commodity;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.views.IDynamicTreeNode;

public class StocksNode implements IDynamicTreeNode {

    public static final String ID = "net.sf.jmoney.stocks.stocks"; //$NON-NLS-1$

	public boolean hasChildren() {
		Session session = JMoneyPlugin.getDefault().getSession();
		if (session != null) {
			for (Commodity commodity: session.getCommodityCollection()) {
				if (commodity instanceof Stock) {
					return true;
				}
			}
			return false;
		} else {
			return false;
		}
	}

	public Collection<Object> getChildren() {
		Session session = JMoneyPlugin.getDefault().getSession();
		ArrayList<Object> children = new ArrayList<Object>();
		if (session != null) {
			for (Commodity commodity: session.getCommodityCollection()) {
				if (commodity instanceof Stock) {
					children.add(commodity);
				}
			}
		}
		return children;
	}
}

