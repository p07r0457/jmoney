/*
 *
 *  JMoney - A Personal Finance Manager
 *  Copyright (c) 2009 Nigel Westbury <westbury@users.sourceforge.net>
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

package net.sf.jmoney.stocks;

import net.sf.jmoney.model2.Account;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.stocks.model.Stock;
import net.sf.jmoney.stocks.model.StockAccount;
import net.sf.jmoney.stocks.pages.StockDetailsEditor;
import net.sf.jmoney.views.AccountEditor;
import net.sf.jmoney.views.AccountEditorInput;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;

public class ShowStockDetailsHandler extends AbstractHandler {
	
	private AccountEditor editor;

	public ShowStockDetailsHandler(AccountEditor editor) {
		this.editor = editor;
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);

        AccountEditorInput cInput = (AccountEditorInput)editor.getEditorInput();
        DatastoreManager sessionManager = (DatastoreManager)editor.getSite().getPage().getInput();
        Account account = sessionManager.getSession().getAccountByFullName(cInput.getFullAccountName());
		StockAccount stockAccount = (StockAccount)account;
		
		final FilteredStocksSelectionDialog dialog = new FilteredStocksSelectionDialog(shell, stockAccount);
		final int resultCode = dialog.open();
		if (resultCode == Window.OK) {
			Object[] result = dialog.getResult();
			for (Object selectedObject : result) {
				Stock stock = (Stock) selectedObject;
				IEditorPart stockDetailsEditor = new StockDetailsEditor(stock);
				try {
					editor.addPage(stockDetailsEditor, stock.getName());
				} catch (PartInitException e) {
					throw new ExecutionException("Cannot create editor for " + stock.getName() + " details", e);
				}
			}
		}
		
		return null;
	}
}