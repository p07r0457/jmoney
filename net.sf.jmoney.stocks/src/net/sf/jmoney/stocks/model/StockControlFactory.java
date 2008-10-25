package net.sf.jmoney.stocks.model;

import net.sf.jmoney.model2.ExtendableObject;
import net.sf.jmoney.model2.IPropertyControl;
import net.sf.jmoney.model2.IReferenceControlFactory;
import net.sf.jmoney.model2.PropertyControlFactory;
import net.sf.jmoney.model2.ScalarPropertyAccessor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @param <P>
 * 		the class of objects that contain this property
 */
public abstract class StockControlFactory<P> extends PropertyControlFactory<Stock> implements IReferenceControlFactory<P, Stock> {

	public IPropertyControl createPropertyControl(Composite parent, final ScalarPropertyAccessor<Stock> propertyAccessor) {

		final StockControl<Stock> control = new StockControl<Stock>(parent, null, Stock.class);
		
		return new IPropertyControl<ExtendableObject>() {

			public Control getControl() {
				return control;
			}

			public void load(ExtendableObject object) {
				// TODO Auto-generated method stub

		        control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
		        control.setStock(object.getPropertyValue(propertyAccessor));
	}

			public void save() {
				// TODO Auto-generated method stub

			}};
	}

	public Stock getDefaultValue() {
		return null;
	}

	public boolean isEditable() {
		return true;
	}
}
