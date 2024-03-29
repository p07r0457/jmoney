package net.sf.jmoney.ofx.parser;

/*
 * @(#)SimpleDOMParser.java
 */

import java.io.IOException;
import java.io.Reader;
import java.util.Stack;

/**
 * <code>SimpleDOMParser</code> is a highly-simplified XML DOM
 * parser.
 */
public class SimpleDOMParser {
	private static final int[] cdata_start = {'<', '!', '[', 'C', 'D', 'A', 'T', 'A', '['};
	private static final int[] cdata_end = {']', ']', '>'};

	private Reader reader;
	private Stack<SimpleElement> elements;
	private SimpleElement currentElement;

	public SimpleDOMParser() {
		elements = new Stack<SimpleElement>();
		currentElement = null;
	}

	public SimpleElement parse(Reader reader) throws IOException {
		this.reader = reader;

		// skip xml declaration or DocTypes
		skipPrologs();

		// remove the prepend or trailing white spaces
		String currentTag = readTag();
		while (true) {
			int index;
			String tagName;

			/**
			 * Sometimes we have to peek ahead to the next tag but then find it is a tag
			 * we want to leave for the next time around this loop.  In that case, it
			 * is set in this variable.
			 */
			String nextTag = null;

			if (currentTag.startsWith("</")) {
				// close tag
				tagName = currentTag.substring(2, currentTag.length()-1);

				// no open tag
				if (currentElement == null) {
					throw new IOException("Got close tag '" + tagName +
							"' without open tag.");
				}

				// close tag does not match with open tag
				if (!tagName.equals(currentElement.getTagName())) {
					throw new IOException("Expected close tag for '" +
							currentElement.getTagName() + "' but got '" +
							tagName + "'.");
				}

				if (elements.empty()) {
					// document processing is over
					return currentElement;
				} else {
					// pop up the previous open tag
					currentElement = elements.pop();
				}
			} else {
				// open tag or tag with both open and close tags
				index = currentTag.indexOf(" ");
				if (index < 0) {
					// tag with no attributes
					if (currentTag.endsWith("/>")) {
						// close tag as well
						tagName = currentTag.substring(1, currentTag.length()-2);
						currentTag = "/>";
					} else {
						// open tag
						tagName = currentTag.substring(1, currentTag.length()-1);
						currentTag = "";
					}
				} else {
					// tag with attributes
					tagName = currentTag.substring(1, index);
					currentTag = currentTag.substring(index+1);
				}

				// create new element
				SimpleElement element = new SimpleElement(tagName);

				// parse the attributes
				boolean isTagClosed = false;
				while (currentTag.length() > 0) {
					// remove the prepend or trailing white spaces
					currentTag = currentTag.trim();

					if (currentTag.equals("/>")) {
						// close tag
						isTagClosed = true;
						break;
					} else if (currentTag.equals(">")) {
						// open tag
						break;
					}

					index = currentTag.indexOf("=");
					if (index < 0) {
						throw new IOException("Invalid attribute for tag '" +
								tagName + "'.");
					}

					// get attribute name
					String attributeName = currentTag.substring(0, index);
					currentTag = currentTag.substring(index+1);

					// get attribute value
					String attributeValue;
					boolean isQuoted = true;
					if (currentTag.startsWith("\"")) {
						index = currentTag.indexOf('"', 1);
					} else if (currentTag.startsWith("'")) {
						index = currentTag.indexOf('\'', 1);
					} else {
						isQuoted = false;
						index = currentTag.indexOf(' ');
						if (index < 0) {
							index = currentTag.indexOf('>');
							if (index < 0) {
								index = currentTag.indexOf('/');
							}
						}
					}

					if (index < 0) {
						throw new IOException("Invalid attribute for tag '" +
								tagName + "'.");
					}

					if (isQuoted) {
						attributeValue = currentTag.substring(1, index);
					} else {
						attributeValue = currentTag.substring(0, index);
					}

					// add attribute to the new element
					element.setAttribute(attributeName, attributeValue);

					currentTag = currentTag.substring(index+1);
				}

				// read the text between the open and close tag
				if (!isTagClosed) {
					element.setText(readText());
					
					if (!element.isEmptyText()) {
						// There may or may not be a close tag
						nextTag = readTag();
						if (nextTag.startsWith("</")) {
							// close tag
							String nextTagName = nextTag.substring(2, nextTag.length()-1);
							if (nextTagName.equals(tagName)) {
								// Set to null as this is no longer a pre-fetched tag
								nextTag = null;
							}
						}
						isTagClosed=true;
					}
				}

				// add new element as a child element of
				// the current element
				if (currentElement != null) {
					currentElement.addChildElement(element);
				}

				if (!isTagClosed) {
					if (currentElement != null) {
						elements.push(currentElement);
					}

					currentElement = element;
				} else if (currentElement == null) {
					// only has one tag in the document
					return element;
				}
			}

			// get tag for next time around
				if (nextTag != null) {
					currentTag = nextTag;
				} else {
					currentTag = readTag();
				}
		}
	}

	private int peek() throws IOException {
		reader.mark(1);
		int result = reader.read();
		reader.reset();

		return result;
	}

	private void peek(int[] buffer) throws IOException {
		reader.mark(buffer.length);
		for (int i=0; i<buffer.length; i++) {
			buffer[i] = reader.read();
		}
		reader.reset();
	}

	private void skipWhitespace() throws IOException {
		char peek = (char)peek();
		while (Character.isWhitespace(peek)||peek!='<') {
			reader.read();
			peek = (char)peek();
		}
	}

	private void skipProlog() throws IOException {
		// skip "<?" or "<!"
		reader.skip(2);

		while (true) {
			int next = peek();

			if (next == '>') {
				reader.read();
				break;
			} else if (next == '<') {
				// nesting prolog
				skipProlog();
			} else {
				reader.read();
			}
		}
	}

	private void skipPrologs() throws IOException {
		while (true) {
			skipWhitespace();

			int[] next = new int[2];
			peek(next);

			if (next[0] != '<') {
				throw new IOException("Expected '<' but got '" + (char)next[0] + "'.");
			}

			if ((next[1] == '?') || (next[1] == '!')) {
				skipProlog();
			} else {
				break;
			}
		}
	}

	/**
	 * 
	 * @return the contents of the stream (excluding preceding whitespace) up to and including the first '>' character,
	 * 			being a string that starts with '<' and ends with '>' 
	 * @throws IOException if the next character (excluding whitespace) is
	 * 			not '<'
	 */
	private String readTag() throws IOException {
		skipWhitespace();

		StringBuffer sb = new StringBuffer();

		int next = peek();
		if (next != '<') {
			throw new IOException("Expected < but got " + (char)next);
		}

		sb.append((char)reader.read());
		while (peek() != '>') {
			sb.append((char)reader.read());
		}
		sb.append((char)reader.read());

		return sb.toString();
	}

	private String readText() throws IOException {
		StringBuffer sb = new StringBuffer();

		int[] next = new int[cdata_start.length];
		peek(next);
		if (compareIntArrays(next, cdata_start) == true) {
			// CDATA
			reader.skip(next.length);

			int[] buffer = new int[cdata_end.length];
			while (true) {
				peek(buffer);

				if (compareIntArrays(buffer, cdata_end) == true) {
					reader.skip(buffer.length);
					break;
				} else {
					sb.append((char)reader.read());
				}
			}
		} else {
			while (peek() != '<') {
				char nextChar = (char)reader.read();
				if (nextChar == '&') {
					reader.mark(4);
					StringBuffer readAhead = new StringBuffer();
					for (int i=0; i<4; i++) {
						char escapeChar = (char)reader.read();
						readAhead.append(escapeChar);
						if (escapeChar == ';') break;
					}

					if (readAhead.toString().equals("amp;")) {
						sb.append('&');
					} else if (readAhead.toString().equals("gt;")) {
							sb.append('>');
					} else if (readAhead.toString().equals("lt;")) {
						sb.append('<');
					} else {
						sb.append('&');
						reader.reset();
					}
				} else {
					sb.append(nextChar);
				}
			}
		}
		return sb.toString();
	}

	private boolean compareIntArrays(int[] a1, int[] a2) {
		if (a1.length != a2.length) {
			return false;
		}

		for (int i=0; i<a1.length; i++) {
			if (a1[i] != a2[i]) {
				return false;
			}
		}

		return true;
	}
}
