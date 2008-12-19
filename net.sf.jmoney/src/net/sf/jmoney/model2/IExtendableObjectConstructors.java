package net.sf.jmoney.model2;


public interface IExtendableObjectConstructors<E extends ExtendableObject> {
	E construct(IObjectKey objectKey, ListKey<? super E> parentKey);
	E construct(IObjectKey objectKey, ListKey<? super E> parentKey, IValues values);
}

