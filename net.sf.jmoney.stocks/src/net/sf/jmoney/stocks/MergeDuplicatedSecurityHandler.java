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

import java.text.MessageFormat;

import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.ExtendablePropertySet;
import net.sf.jmoney.model2.ListPropertyAccessor;
import net.sf.jmoney.model2.ObjectCollection;
import net.sf.jmoney.model2.ScalarPropertyAccessor;
import net.sf.jmoney.model2.SessionInfo;
import net.sf.jmoney.stocks.model.Security;
import net.sf.jmoney.stocks.model.SecurityInfo;
import net.sf.jmoney.stocks.pages.StockDetailsEditor;
import net.sf.jmoney.views.AccountEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class MergeDuplicatedSecurityHandler extends AbstractHandler {
	
	public MergeDuplicatedSecurityHandler(AccountEditor editor) {
	}
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IStructuredSelection selection = (IStructuredSelection)HandlerUtil.getCurrentSelectionChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		Security security1 = (Security)selection.toList().get(0);
		Security security2 = (Security)selection.toList().get(1);
		
		ExtendablePropertySet<? extends Security> propertySet1 = SecurityInfo.getPropertySet().getActualPropertySet(security1.getClass());
		ExtendablePropertySet<? extends Security> propertySet2 = SecurityInfo.getPropertySet().getActualPropertySet(security2.getClass());
		
				if (propertySet1 != propertySet2) {
			throw new ExecutionException(MessageFormat.format("The two securities cannot be merged because they are not of types {0} and {1}.  Only securities of the same type can be merged.", propertySet1.getObjectDescription(), propertySet1.getObjectDescription()));
		}
		
        DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		
		TransactionManager transaction = new TransactionManager(sessionManager);
		
		mergeSecondIntoFirst(transaction, propertySet1, security1, security2);
		
		replaceSecondWithFirst(transaction, SessionInfo.getPropertySet(), transaction.getSession(), propertySet1, security1, security2);

		transaction.commit("Merge Securities");
		
		return null;
	}

	private <S extends Security, E extends ExtendableObject> void mergeSecondIntoFirst(TransactionManager transaction,
			ExtendablePropertySet<S> securityPropertySet,
			S security1, S security2) {
		
	}
		
	private <S extends Security, E extends ExtendableObject> void replaceSecondWithFirst(TransactionManager transaction,
			ExtendablePropertySet<E> thisPropertySet, E thisExtendableObject,
			ExtendablePropertySet<S> securityPropertySet,
			S security1, S security2) {
		
		/*
		 * Look through the scalar values for references to the second security.
		 * Replace with references to the first.
		 */
		for (ScalarPropertyAccessor<?> scalarAccessor: thisPropertySet.getScalarProperties3()) {
			ScalarPropertyAccessor<S> scalarAccessorTyped = scalarAccessor.getTypedAccessor(securityPropertySet);

			if (thisExtendableObject.getPropertyValue(scalarAccessor) == security2) {
				thisExtendableObject.setPropertyValue(scalarAccessorTyped, security1);
			}
		}
    	
		/*
		 * Pass through all the list properties
		 */
		
    	for (ListPropertyAccessor<?> listAccessor: thisPropertySet.getListProperties3()) {
    		processChildElement(transaction, thisExtendableObject,
					securityPropertySet, security1, security2, listAccessor);
    	}

	}

	private <S extends Security, E extends ExtendableObject, C extends ExtendableObject> void processChildElement(TransactionManager transaction,
			E thisExtendableObject,
			ExtendablePropertySet<S> securityPropertySet, S security1,
			S security2, ListPropertyAccessor<C> listAccessor) {
		ExtendablePropertySet<C> childPropertySet = listAccessor.getElementPropertySet();
		ObjectCollection<C> children = thisExtendableObject.getListPropertyValue(listAccessor);
		for (C childExtendableObject : children) {
			replaceSecondWithFirst(transaction, childPropertySet, childExtendableObject,
					securityPropertySet, security1, security2);
		}
	}
}