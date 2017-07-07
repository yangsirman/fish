package fish.json.stream;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

public class JsonWriter implements Closeable, Flushable {

	private final Writer out;

	private final static String[] REPLACEMENT_CHARS;
	private static final String[] HTML_SAFE_REPLACEMENT_CHARS;
	static {
		REPLACEMENT_CHARS = new String[128];
		for (int i = 0; i <= 0x1f; i++) {
			REPLACEMENT_CHARS[i] = String.format("\\u%04x", (int) i);
		}
		REPLACEMENT_CHARS['"'] = "\\\"";
		REPLACEMENT_CHARS['\\'] = "\\\\";
		REPLACEMENT_CHARS['\t'] = "\\t";
		REPLACEMENT_CHARS['\b'] = "\\b";
		REPLACEMENT_CHARS['\n'] = "\\n";
		REPLACEMENT_CHARS['\r'] = "\\r";
		REPLACEMENT_CHARS['\f'] = "\\f";
		HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
		HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
		HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
		HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
		HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
		HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
	}
	private int[] stack = new int[32];
	private int stackSize = 0;
	{

	}

	private String indent;
	private String separator = ":";
	private boolean lenient;
	private boolean htmlSafe;
	private String deferredName;
	private boolean serializeNulls = true;

	public JsonWriter(Writer out) {
		if (out == null) {
			throw new NullPointerException("out == null");
		}
		this.out = out;
	}

	public void setIndent(String indent) {
		if (indent.length() == 0) {
			this.indent = null;
			this.separator = ":";
		} else {
			this.indent = indent;
			this.separator = ":";
		}
	}

	public final void setLeninet(boolean v) {
		this.lenient = v;
	}

	public final boolean isLeninet() {
		return this.lenient;
	}

	public final void setHtmlSafe(boolean s) {
		this.htmlSafe = s;
	}

	public final boolean isHtmlSafe(boolean s) {
		return this.htmlSafe;
	}

	public boolean getSerializeNulls() {
		return serializeNulls;
	}

	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	private void push(int newTop) {
		if (this.stack.length == this.stackSize) {
			int[] newStack = new int[stackSize * 2];
			System.arraycopy(stack, 0, newStack, 0, stackSize);
			stack = newStack;
		}
		stack[stackSize++] = newTop;
	}

	private int peek() {
		if (stackSize == 0) {
			throw new IllegalStateException("JsonWriter is closed.");
		}
		return stack[stackSize - 1];
	}

	private void replaceTop(int topOfStack) {
		stack[stackSize - 1] = topOfStack;
	}

	public JsonWriter beginArray() {
		writeDeferredName();
		return open(JsonScope.EMPTY_ARRAY, "[");
	}

	public JsonWriter endArray() {
		return this.close(JsonScope.EMPTY_ARRAY, JsonScope.NONEMPTY_ARRAY, "]");
	}

	public JsonWriter beginObject() {
		this.writeDeferredName();
		return open(JsonScope.EMPTY_OBJECT, "{");
	}

	public JsonWriter endObject() {
		return close(JsonScope.EMPTY_OBJECT, JsonScope.NONEMPTY_OBJECT, "}");
	}

	public JsonWriter name(String name) {
		if (name == null) {
			throw new NullPointerException("name == null");
		}
		if (deferredName != null) {
			throw new IllegalStateException();
		}
		if (stackSize == 0) {
			throw new IllegalStateException("JsonWriter is closed.");
		}
		this.deferredName = name;
		return this;
	}

	private JsonWriter open(int empty, String string) {
		beforeValue();
		push(empty);
		try {
			out.write(string);
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
		return this;
	}

	private JsonWriter close(int empty, int nonempty, String closeBracket) {
		int context = peek();
		if (context != nonempty && context != empty) {
			throw new IllegalStateException("Nesting problem.");
		}
		if (deferredName != null) {
			throw new IllegalStateException("Dangling name: " + deferredName);
		}

		stackSize--;
		if (context == nonempty) {
			newline();
		}
		try {
			out.write(closeBracket);
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
		return this;
	}

	private void writeDeferredName() {
		if (deferredName != null) {
			beforeName();
			string(deferredName);
			deferredName = null;
		}

	}

	public JsonWriter value(String value) {
		if (value == null) {
			this.nullValue();
		}
		this.writeDeferredName();
		this.beforeValue();
		string(value);
		return this;
	}

	public JsonWriter nullValue() {
		if (this.deferredName != null) {
			if (this.serializeNulls)
				this.writeDeferredName();
			else {
				deferredName = null;
				return this;
			}
		}
		this.beforeValue();
		try {
			out.write("null");
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
		return this;
	}

	public JsonWriter value(Boolean value) {
		if (value)
			return this.value("true");
		else
			return this.value("false");
	}
	
	public JsonWriter value(Number value) {
		if (value!=null)
			return this.value(value.toString());
		else
			return this.nullValue();
	}

	private void string(String value) {
		String[] replacements = htmlSafe ? HTML_SAFE_REPLACEMENT_CHARS : REPLACEMENT_CHARS;
	    try {
			out.write("\"");
			int last = 0;
		    int length = value.length();
		    for (int i = 0; i < length; i++) {
		      char c = value.charAt(i);
		      String replacement;
		      if (c < 128) {
		        replacement = replacements[c];
		        if (replacement == null) {
		          continue;
		        }
		      } else if (c == '\u2028') {
		        replacement = "\\u2028";
		      } else if (c == '\u2029') {
		        replacement = "\\u2029";
		      } else {
		        continue;
		      }
		      if (last < i) {
		        out.write(value, last, i - last);
		      }
		      out.write(replacement);
		      last = i + 1;
		    }
		    if (last < length) {
		      out.write(value, last, length - last);
		    }
		    out.write("\"");
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
	    
	}

	private void newline() {
		if (indent == null)
			return;
		try {
			out.write("\n");
			for (int i = 1, size = stackSize; i < size; i++) {
				out.write(indent);
			}
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
	}

	private void beforeName() {
		int context = peek();
		if (context == JsonScope.NONEMPTY_OBJECT) {
			try {
				out.write(",");
			} catch (IOException e) {
				throw new JsonIOException(e);
			}
		} else if (context != JsonScope.EMPTY_OBJECT) { // not in an object!
			throw new IllegalStateException("Nesting problem.");
		}
		newline();
		replaceTop(JsonScope.DANGLING_NAME);
	}

	private void beforeValue() {
		try {
			switch (peek()) {
			case JsonScope.NONEMPTY_DOCUMENT:
				if (!lenient) {
					throw new IllegalStateException(
							"JSON must have only one top-level value.");
				}
				// fall-through
			case JsonScope.EMPTY_DOCUMENT: // first in document
				replaceTop(JsonScope.NONEMPTY_DOCUMENT);
				break;

			case JsonScope.EMPTY_ARRAY: // first in array
				replaceTop(JsonScope.NONEMPTY_ARRAY);
				newline();
				break;

			case JsonScope.NONEMPTY_ARRAY: // another in array
				out.append(',');
				newline();
				break;

			case JsonScope.DANGLING_NAME: // value for name
				out.append(separator);
				replaceTop(JsonScope.NONEMPTY_OBJECT);
				break;

			default:
				throw new IllegalStateException("Nesting problem.");
			}
		} catch (IOException e) {
			throw new JsonIOException(e);
		}

	}


	public void flush() {
		if (stackSize == 0) {
			throw new IllegalStateException("JsonWriter is closed.");
		}
		try {
			out.flush();
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
		
	}

	public void close(){
		try {
			out.close();
		} catch (IOException e) {
			throw new JsonIOException(e);
		}

		int size = stackSize;
		if (size > 1 || size == 1
				&& stack[size - 1] != JsonScope.NONEMPTY_DOCUMENT) {
			throw new JsonIOException("Incomplete document");
		}
		stackSize = 0;
	}

}
