/*
 *
 *  JMoney - A Personal Finance Manager
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

package net.sf.jmoney.fields;

import net.sf.jmoney.model2.Account;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;

public abstract class AccountComposite extends Composite {
	
	public AccountComposite(Composite parent, int style) {
		super(parent, style);
	}
	
	public abstract Account getAccount();
	public abstract void setAccount(Account account);
	public abstract void rememberChoice();
	public abstract void init(IMemento memento);
	public abstract void saveState(IMemento memento);
}