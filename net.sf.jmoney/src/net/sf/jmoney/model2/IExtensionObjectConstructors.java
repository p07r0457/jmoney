package net.sf.jmoney.model2;


public interface IExtensionObjectConstructors<E extends ExtensionObject> {
	E construct(ExtendableObject extendedObject);
	E construct(ExtendableObject extendedObject, IValues values);
}

