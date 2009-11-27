package net.sf.jmoney.model2;

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
public abstract class CommodityControlFactory<P> extends PropertyControlFactory<Commodity> implements IReferenceControlFactory<P, Commodity> {

	public IPropertyControl createPropertyControl(Composite parent, final ScalarPropertyAccessor<Commodity> propertyAccessor) {

		final CommodityControl<Commodity> control = new CommodityControl<Commodity>(parent, null, Commodity.class);
		
		return new IPropertyControl<ExtendableObject>() {

			private ExtendableObject fObject;

			public Control getControl() {
				return control;
			}

			public void load(ExtendableObject object) {
		        fObject = object;
		        
		        control.setSession(object.getSession(), propertyAccessor.getClassOfValueObject());
		        control.setCommodity(object.getPropertyValue(propertyAccessor));
			}

			public void save() {
				Commodity commodity = control.getCommodity();
				fObject.setPropertyValue(propertyAccessor, commodity);
			}};
	}

	public Commodity getDefaultValue() {
		return null;
	}

	public boolean isEditable() {
		return true;
	}
}
