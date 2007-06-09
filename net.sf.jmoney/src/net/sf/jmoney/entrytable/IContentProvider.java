package net.sf.jmoney.entrytable;

public interface IContentProvider {

	int getRowCount();

	EntryData getElement(int rowNumber);

	int indexOf(EntryData entryData);
}
