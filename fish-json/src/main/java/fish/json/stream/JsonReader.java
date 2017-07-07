package fish.json.stream;

import java.io.*;

public class JsonReader implements Closeable {

	private static final char[] NON_EXECUTE_PREFIX = ")]}'\n".toCharArray();
	private static final long MIN_INCOMPLETE_INTEGER = Long.MIN_VALUE / 10;

	private static final int PEEKED_NONE = 0;
	private static final int PEEKED_BEGIN_OBJECT = 1;
	private static final int PEEKED_END_OBJECT = 2;
	private static final int PEEKED_BEGIN_ARRAY = 3;
	private static final int PEEKED_END_ARRAY = 4;
	private static final int PEEKED_TRUE = 5;
	private static final int PEEKED_FALSE = 6;
	private static final int PEEKED_NULL = 7;
	private static final int PEEKED_SINGLE_QUOTED = 8;
	private static final int PEEKED_DOUBLE_QUOTED = 9;
	private static final int PEEKED_UNQUOTED = 10;
	private static final int PEEKED_BUFFERED = 11;
	private static final int PEEKED_SINGLE_QUOTED_NAME = 12;
	private static final int PEEKED_DOUBLE_QUOTED_NAME = 13;
	private static final int PEEKED_UNQUOTED_NAME = 14;
	private static final int PEEKED_LONG = 15;
	private static final int PEEKED_NUMBER = 16;
	private static final int PEEKED_EOF = 17;

	private static final int NUMBER_CHAR_NONE = 0;
	private static final int NUMBER_CHAR_SIGN = 1;
	private static final int NUMBER_CHAR_DIGIT = 2;
	private static final int NUMBER_CHAR_DECIMAL = 3;
	private static final int NUMBER_CHAR_FRACTION_DIGIT = 4;
	private static final int NUMBER_CHAR_EXP_E = 5;
	private static final int NUMBER_CHAR_EXP_SIGN = 6;
	private static final int NUMBER_CHAR_EXP_DIGIT = 7;

	private final Reader in;
	private boolean lenient = false;
	private final char[] buffer = new char[1024];

	private int pos = 0;
	private int limit = 0;

	private int lineNumber = 0;
	private int lineStart = 0;
	int peeked = PEEKED_NONE;

	private long peekedLong;

	private int peekedNumberLength;

	private String peekedString;
	private int[] stack = new int[32];
	private int stackSize = 0;
	{
		stack[stackSize++] = JsonScope.EMPTY_DOCUMENT;
	}

	private String[] pathNames = new String[32];
	private int[] pathIndices = new int[32];

	public JsonReader(Reader in) {
		if (in == null) {
			throw new NullPointerException("in == null");
		}
		this.in = in;
	}

	public void close() throws IOException {
		peeked = PEEKED_NONE;
		stack[0] = JsonScope.CLOSED;
		stackSize = 1;
		in.close();
	}

	public boolean isLenient() {
		return lenient;
	}

	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	public void beginArray() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}

		if (p == PEEKED_BEGIN_ARRAY) {
			push(JsonScope.EMPTY_ARRAY);
			pathIndices[stackSize - 1] = 0;
			peeked = PEEKED_NONE;
		} else {
			throw new IllegalStateException("Expected BEGIN_ARRAY but was "
					+ peek() + locationString());
		}
	}

	public void endArray() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		if (p == PEEKED_END_ARRAY) {
			stackSize--;
			pathIndices[stackSize - 1]++;
			peeked = PEEKED_NONE;
		} else {
			throw new IllegalStateException("Expected END_ARRAY but was "
					+ peek() + locationString());
		}
	}

	public void beginObject() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		if (p == PEEKED_BEGIN_OBJECT) {
			push(JsonScope.EMPTY_OBJECT);
			peeked = PEEKED_NONE;
		} else {
			throw new IllegalStateException("Expected BEGIN_OBJECT but was "
					+ peek() + locationString());
		}
	}

	public void endObject() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}

		if (p == PEEKED_END_OBJECT) {
			stackSize--;
			pathNames[stackSize] = null; // Free the last path name so that it
											// can be garbage collected!
			pathIndices[stackSize - 1]++;
			peeked = PEEKED_NONE;
		} else {
			throw new IllegalStateException("Expected END_OBJECT but was "
					+ peek() + locationString());
		}
	}

	public boolean hasNext() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		return p != PEEKED_END_OBJECT && p != PEEKED_END_ARRAY;
	}

	private String locationString() {
		int line = lineNumber + 1;
		int column = pos - lineStart + 1;
		return " at line " + line + " column " + column + " path " + getPath();
	}

	public String getPath() {
		StringBuilder result = new StringBuilder().append('$');
		for (int i = 0, size = stackSize; i < size; i++) {
			switch (stack[i]) {
			case JsonScope.EMPTY_ARRAY:
			case JsonScope.NONEMPTY_ARRAY:
				result.append('[').append(pathIndices[i]).append(']');
				break;

			case JsonScope.EMPTY_OBJECT:
			case JsonScope.DANGLING_NAME:
			case JsonScope.NONEMPTY_OBJECT:
				result.append('.');
				if (pathNames[i] != null) {
					result.append(pathNames[i]);
				}
				break;

			case JsonScope.NONEMPTY_DOCUMENT:
			case JsonScope.EMPTY_DOCUMENT:
			case JsonScope.CLOSED:
				break;
			}
		}
		return result.toString();
	}

	public JsonToken peek() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}

		switch (p) {
		case PEEKED_BEGIN_OBJECT:
			return JsonToken.BEGIN_OBJECT;
		case PEEKED_END_OBJECT:
			return JsonToken.END_OBJECT;
		case PEEKED_BEGIN_ARRAY:
			return JsonToken.BEGIN_ARRAY;
		case PEEKED_END_ARRAY:
			return JsonToken.END_ARRAY;
		case PEEKED_SINGLE_QUOTED_NAME:
		case PEEKED_DOUBLE_QUOTED_NAME:
		case PEEKED_UNQUOTED_NAME:
			return JsonToken.NAME;
		case PEEKED_TRUE:
		case PEEKED_FALSE:
			return JsonToken.BOOLEAN;
		case PEEKED_NULL:
			return JsonToken.NULL;
		case PEEKED_SINGLE_QUOTED:
		case PEEKED_DOUBLE_QUOTED:
		case PEEKED_UNQUOTED:
		case PEEKED_BUFFERED:
			return JsonToken.STRING;
		case PEEKED_LONG:
		case PEEKED_NUMBER:
			return JsonToken.NUMBER;
		case PEEKED_EOF:
			return JsonToken.END_DOCUMENT;
		default:
			throw new AssertionError();
		}
	}

	private int doPeek() {
		int peekStack = stack[stackSize - 1];
		if (peekStack == JsonScope.EMPTY_ARRAY) {
			stack[stackSize - 1] = JsonScope.NONEMPTY_ARRAY;
		} else if (peekStack == JsonScope.NONEMPTY_ARRAY) {
			// Look for a comma before the next element.
			int c = nextNonWhitespace(true);
			switch (c) {
			case ']':
				return peeked = PEEKED_END_ARRAY;
			case ';':
				checkLenient(); // fall-through
			case ',':
				break;
			default:
				throw new RuntimeException("Unterminated array");
			}
		} else if (peekStack == JsonScope.EMPTY_OBJECT
				|| peekStack == JsonScope.NONEMPTY_OBJECT) {
			stack[stackSize - 1] = JsonScope.DANGLING_NAME;
			// Look for a comma before the next element.
			if (peekStack == JsonScope.NONEMPTY_OBJECT) {
				int c = nextNonWhitespace(true);
				switch (c) {
				case '}':
					return peeked = PEEKED_END_OBJECT;
				case ';':
					checkLenient(); // fall-through
				case ',':
					break;
				default:
					throw new RuntimeException("Unterminated object");
				}
			}
			int c = nextNonWhitespace(true);
			switch (c) {
			case '"':
				return peeked = PEEKED_DOUBLE_QUOTED_NAME;
			case '\'':
				checkLenient();
				return peeked = PEEKED_SINGLE_QUOTED_NAME;
			case '}':
				if (peekStack != JsonScope.NONEMPTY_OBJECT) {
					return peeked = PEEKED_END_OBJECT;
				} else {
					throw new RuntimeException("Expected name");
				}
			default:
				checkLenient();
				pos--; // Don't consume the first character in an unquoted
						// string.
				if (isLiteral((char) c)) {
					return peeked = PEEKED_UNQUOTED_NAME;
				} else {
					throw new RuntimeException("Expected name");
				}
			}
		} else if (peekStack == JsonScope.DANGLING_NAME) {
			stack[stackSize - 1] = JsonScope.NONEMPTY_OBJECT;
			// Look for a colon before the value.
			int c = nextNonWhitespace(true);
			switch (c) {
			case ':':
				break;
			case '=':
				checkLenient();
				if ((pos < limit || fillBuffer(1)) && buffer[pos] == '>') {
					pos++;
				}
				break;
			default:
				throw new RuntimeException("Expected ':'");
			}
		} else if (peekStack == JsonScope.EMPTY_DOCUMENT) {
			if (lenient) {
				consumeNonExecutePrefix();
			}
			stack[stackSize - 1] = JsonScope.NONEMPTY_DOCUMENT;
		} else if (peekStack == JsonScope.NONEMPTY_DOCUMENT) {
			int c = nextNonWhitespace(false);
			if (c == -1) {
				return peeked = PEEKED_EOF;
			} else {
				checkLenient();
				pos--;
			}
		} else if (peekStack == JsonScope.CLOSED) {
			throw new IllegalStateException("JsonReader is closed");
		}

		int c = nextNonWhitespace(true);
		switch (c) {
		case ']':
			if (peekStack == JsonScope.EMPTY_ARRAY) {
				return peeked = PEEKED_END_ARRAY;
			}
			// fall-through to handle ",]"
		case ';':
		case ',':
			// In lenient mode, a 0-length literal in an array means 'null'.
			if (peekStack == JsonScope.EMPTY_ARRAY
					|| peekStack == JsonScope.NONEMPTY_ARRAY) {
				checkLenient();
				pos--;
				return peeked = PEEKED_NULL;
			} else {
				throw new RuntimeException("Unexpected value");
			}
		case '\'':
			checkLenient();
			return peeked = PEEKED_SINGLE_QUOTED;
		case '"':
			return peeked = PEEKED_DOUBLE_QUOTED;
		case '[':
			return peeked = PEEKED_BEGIN_ARRAY;
		case '{':
			return peeked = PEEKED_BEGIN_OBJECT;
		default:
			pos--; // Don't consume the first character in a literal value.
		}

		int result = peekKeyword();
		if (result != PEEKED_NONE) {
			return result;
		}

		result = peekNumber();
		if (result != PEEKED_NONE) {
			return result;
		}

		if (!isLiteral(buffer[pos])) {
			throw new RuntimeException("Expected value");
		}

		checkLenient();
		return peeked = PEEKED_UNQUOTED;
	}

	private void consumeNonExecutePrefix() {
		nextNonWhitespace(true);
		pos--;

		if (pos + NON_EXECUTE_PREFIX.length > limit
				&& !fillBuffer(NON_EXECUTE_PREFIX.length)) {
			return;
		}

		for (int i = 0; i < NON_EXECUTE_PREFIX.length; i++) {
			if (buffer[pos + i] != NON_EXECUTE_PREFIX[i]) {
				return; // not a security token!
			}
		}

		// we consumed a security token!
		pos += NON_EXECUTE_PREFIX.length;
	}

	private int nextNonWhitespace(boolean throwOnEof) {
		char[] buffer = this.buffer;
		int p = pos;
		int l = limit;
		while (true) {
			if (p == l) {
				pos = p;
				if (!fillBuffer(1)) {
					break;
				}
				p = pos;
				l = limit;
			}

			int c = buffer[p++];
			if (c == '\n') {
				lineNumber++;
				lineStart = p;
				continue;
			} else if (c == ' ' || c == '\r' || c == '\t') {
				continue;
			}

			if (c == '/') {
				pos = p;
				if (p == l) {
					pos--; // push back '/' so it's still in the buffer when
							// this method returns
					boolean charsLoaded = fillBuffer(2);
					pos++; // consume the '/' again
					if (!charsLoaded) {
						return c;
					}
				}

				checkLenient();
				char peek = buffer[pos];
				switch (peek) {
				case '*':
					// skip a /* c-style comment */
					pos++;
					if (!skipTo("*/")) {
						throw new RuntimeException("Unterminated comment");
					}
					p = pos + 2;
					l = limit;
					continue;

				case '/':
					// skip a // end-of-line comment
					pos++;
					skipToEndOfLine();
					p = pos;
					l = limit;
					continue;

				default:
					return c;
				}
			} else if (c == '#') {
				pos = p;
				/*
				 * Skip a # hash end-of-line comment. The JSON RFC doesn't
				 * specify this behaviour, but it's required to parse existing
				 * documents. See http://b/2571423.
				 */
				checkLenient();
				skipToEndOfLine();
				p = pos;
				l = limit;
			} else {
				pos = p;
				return c;
			}
		}
		if (throwOnEof) {
			throw new RuntimeException("End of input" + locationString());
		} else {
			return -1;
		}
	}

	private int peekNumber() {
		char[] buffer = this.buffer;
		int p = pos;
		int l = limit;

		long value = 0; // Negative to accommodate Long.MIN_VALUE more easily.
		boolean negative = false;
		boolean fitsInLong = true;
		int last = NUMBER_CHAR_NONE;

		int i = 0;

		charactersOfNumber: for (; true; i++) {
			if (p + i == l) {
				if (i == buffer.length) {
					// Though this looks like a well-formed number, it's too
					// long to continue reading. Give up
					// and let the application handle this as an unquoted
					// literal.
					return PEEKED_NONE;
				}
				if (!fillBuffer(i + 1)) {
					break;
				}
				p = pos;
				l = limit;
			}

			char c = buffer[p + i];
			switch (c) {
			case '-':
				if (last == NUMBER_CHAR_NONE) {
					negative = true;
					last = NUMBER_CHAR_SIGN;
					continue;
				} else if (last == NUMBER_CHAR_EXP_E) {
					last = NUMBER_CHAR_EXP_SIGN;
					continue;
				}
				return PEEKED_NONE;

			case '+':
				if (last == NUMBER_CHAR_EXP_E) {
					last = NUMBER_CHAR_EXP_SIGN;
					continue;
				}
				return PEEKED_NONE;

			case 'e':
			case 'E':
				if (last == NUMBER_CHAR_DIGIT
						|| last == NUMBER_CHAR_FRACTION_DIGIT) {
					last = NUMBER_CHAR_EXP_E;
					continue;
				}
				return PEEKED_NONE;

			case '.':
				if (last == NUMBER_CHAR_DIGIT) {
					last = NUMBER_CHAR_DECIMAL;
					continue;
				}
				return PEEKED_NONE;

			default:
				if (c < '0' || c > '9') {
					if (!isLiteral(c)) {
						break charactersOfNumber;
					}
					return PEEKED_NONE;
				}
				if (last == NUMBER_CHAR_SIGN || last == NUMBER_CHAR_NONE) {
					value = -(c - '0');
					last = NUMBER_CHAR_DIGIT;
				} else if (last == NUMBER_CHAR_DIGIT) {
					if (value == 0) {
						return PEEKED_NONE; // Leading '0' prefix is not allowed
											// (since it could be octal).
					}
					long newValue = value * 10 - (c - '0');
					fitsInLong &= value > MIN_INCOMPLETE_INTEGER
							|| (value == MIN_INCOMPLETE_INTEGER && newValue < value);
					value = newValue;
				} else if (last == NUMBER_CHAR_DECIMAL) {
					last = NUMBER_CHAR_FRACTION_DIGIT;
				} else if (last == NUMBER_CHAR_EXP_E
						|| last == NUMBER_CHAR_EXP_SIGN) {
					last = NUMBER_CHAR_EXP_DIGIT;
				}
			}
		}

		// We've read a complete number. Decide if it's a PEEKED_LONG or a
		// PEEKED_NUMBER.
		if (last == NUMBER_CHAR_DIGIT && fitsInLong
				&& (value != Long.MIN_VALUE || negative)
				&& (value != 0 || false == negative)) {
			peekedLong = negative ? value : -value;
			pos += i;
			return peeked = PEEKED_LONG;
		} else if (last == NUMBER_CHAR_DIGIT
				|| last == NUMBER_CHAR_FRACTION_DIGIT
				|| last == NUMBER_CHAR_EXP_DIGIT) {
			peekedNumberLength = i;
			return peeked = PEEKED_NUMBER;
		} else {
			return PEEKED_NONE;
		}
	}

	private int peekKeyword() {
		char c = buffer[pos];
		String keyword;
		String keywordUpper;
		int peeking;
		if (c == 't' || c == 'T') {
			keyword = "true";
			keywordUpper = "TRUE";
			peeking = PEEKED_TRUE;
		} else if (c == 'f' || c == 'F') {
			keyword = "false";
			keywordUpper = "FALSE";
			peeking = PEEKED_FALSE;
		} else if (c == 'n' || c == 'N') {
			keyword = "null";
			keywordUpper = "NULL";
			peeking = PEEKED_NULL;
		} else {
			return PEEKED_NONE;
		}

		// Confirm that chars [1..length) match the keyword.
		int length = keyword.length();
		for (int i = 1; i < length; i++) {
			if (pos + i >= limit && !fillBuffer(i + 1)) {
				return PEEKED_NONE;
			}
			c = buffer[pos + i];
			if (c != keyword.charAt(i) && c != keywordUpper.charAt(i)) {
				return PEEKED_NONE;
			}
		}

		if ((pos + length < limit || fillBuffer(length + 1))
				&& isLiteral(buffer[pos + length])) {
			return PEEKED_NONE; // Don't match trues, falsey or nullsoft!
		}

		// We've found the keyword followed either by EOF or by a non-literal
		// character.
		pos += length;
		return peeked = peeking;
	}

	private void checkLenient() {
		if (!lenient) {
			throw new RuntimeException(
					"Use JsonReader.setLenient(true) to accept malformed JSON");
		}
	}

	private void skipToEndOfLine() {
		while (this.pos < this.limit || fillBuffer(1)) {
			char c = buffer[pos++];
			if (c == '\n') {
				this.lineNumber++;
				this.lineStart = pos;
				break;
			} else if (c == '\r') {
				break;
			}
		}
	}

	private boolean fillBuffer(int minimum) {
		char[] buffer = this.buffer;
		lineStart -= pos;
		if (limit != pos) {
			limit -= pos;
			System.arraycopy(buffer, pos, buffer, 0, limit);
		} else {
			limit = 0;
		}

		pos = 0;
		int total;
		try {
			while ((total = in.read(buffer, limit, buffer.length - limit)) != -1) {
				limit += total;

				// if this is the first read, consume an optional byte order
				// mark (BOM) if it exists
				if (lineNumber == 0 && lineStart == 0 && limit > 0
						&& buffer[0] == '\ufeff') {
					pos++;
					lineStart++;
					minimum++;
				}

				if (limit >= minimum) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new JsonIOException(e);
		}
		return false;
	}

	private boolean skipTo(String toFind) {
		int length = toFind.length();
		outer: for (; pos + length <= limit || fillBuffer(length); pos++) {
			if (buffer[pos] == '\n') {
				lineNumber++;
				lineStart = pos + 1;
				continue;
			}
			for (int c = 0; c < length; c++) {
				if (buffer[pos + c] != toFind.charAt(c)) {
					continue outer;
				}
			}
			return true;
		}
		return false;
	}

	private boolean isLiteral(char c) {
		switch (c) {
		case '/':
		case '\\':
		case ';':
		case '#':
		case '=':
			checkLenient(); // fall-through
		case '{':
		case '}':
		case '[':
		case ']':
		case ':':
		case ',':
		case ' ':
		case '\t':
		case '\f':
		case '\r':
		case '\n':
			return false;
		default:
			return true;
		}
	}

	private String nextQuotedValue(char quote) {
		// Like nextNonWhitespace, this uses locals 'p' and 'l' to save
		// inner-loop field access.
		char[] buffer = this.buffer;
		StringBuilder builder = null;
		while (true) {
			int p = pos;
			int l = limit;
			/* the index of the first character not yet appended to the builder. */
			int start = p;
			while (p < l) {
				int c = buffer[p++];

				if (c == quote) {
					pos = p;
					int len = p - start - 1;
					if (builder == null) {
						return new String(buffer, start, len);
					} else {
						builder.append(buffer, start, len);
						return builder.toString();
					}
				} else if (c == '\\') {
					pos = p;
					int len = p - start - 1;
					if (builder == null) {
						int estimatedLength = (len + 1) * 2;
						builder = new StringBuilder(Math.max(estimatedLength,
								16));
					}
					builder.append(buffer, start, len);
					builder.append(readEscapeCharacter());
					p = pos;
					l = limit;
					start = p;
				} else if (c == '\n') {
					lineNumber++;
					lineStart = p;
				}
			}

			if (builder == null) {
				int estimatedLength = (p - start) * 2;
				builder = new StringBuilder(Math.max(estimatedLength, 16));
			}
			builder.append(buffer, start, p - start);
			pos = p;
			if (!fillBuffer(1)) {
				throw new RuntimeException("Unterminated string");
			}
		}
	}

	/**
	 * Returns an unquoted value as a string.
	 */
	@SuppressWarnings("fallthrough")
	private String nextUnquotedValue() {
		StringBuilder builder = null;
		int i = 0;

		findNonLiteralCharacter: while (true) {
			for (; pos + i < limit; i++) {
				switch (buffer[pos + i]) {
				case '/':
				case '\\':
				case ';':
				case '#':
				case '=':
					checkLenient(); // fall-through
				case '{':
				case '}':
				case '[':
				case ']':
				case ':':
				case ',':
				case ' ':
				case '\t':
				case '\f':
				case '\r':
				case '\n':
					break findNonLiteralCharacter;
				}
			}

			// Attempt to load the entire literal into the buffer at once.
			if (i < buffer.length) {
				if (fillBuffer(i + 1)) {
					continue;
				} else {
					break;
				}
			}

			// use a StringBuilder when the value is too long. This is too long
			// to be a number!
			if (builder == null) {
				builder = new StringBuilder(Math.max(i, 16));
			}
			builder.append(buffer, pos, i);
			pos += i;
			i = 0;
			if (!fillBuffer(1)) {
				break;
			}
		}

		String result = (null == builder) ? new String(buffer, pos, i)
				: builder.append(buffer, pos, i).toString();
		pos += i;
		return result;
	}

	private void skipQuotedValue(char quote) {
		// Like nextNonWhitespace, this uses locals 'p' and 'l' to save
		// inner-loop field access.
		char[] buffer = this.buffer;
		do {
			int p = pos;
			int l = limit;
			/* the index of the first character not yet appended to the builder. */
			while (p < l) {
				int c = buffer[p++];
				if (c == quote) {
					pos = p;
					return;
				} else if (c == '\\') {
					pos = p;
					readEscapeCharacter();
					p = pos;
					l = limit;
				} else if (c == '\n') {
					lineNumber++;
					lineStart = p;
				}
			}
			pos = p;
		} while (fillBuffer(1));
		throw new RuntimeException("Unterminated string");
	}

	private char readEscapeCharacter() {
		if (pos == limit && !fillBuffer(1)) {
			throw new RuntimeException("Unterminated escape sequence");
		}

		char escaped = buffer[pos++];
		switch (escaped) {
		case 'u':
			if (pos + 4 > limit && !fillBuffer(4)) {
				throw new RuntimeException("Unterminated escape sequence");
			}
			// Equivalent to Integer.parseInt(stringPool.get(buffer, pos, 4),
			// 16);
			char result = 0;
			for (int i = pos, end = i + 4; i < end; i++) {
				char c = buffer[i];
				result <<= 4;
				if (c >= '0' && c <= '9') {
					result += (c - '0');
				} else if (c >= 'a' && c <= 'f') {
					result += (c - 'a' + 10);
				} else if (c >= 'A' && c <= 'F') {
					result += (c - 'A' + 10);
				} else {
					throw new NumberFormatException("\\u"
							+ new String(buffer, pos, 4));
				}
			}
			pos += 4;
			return result;

		case 't':
			return '\t';

		case 'b':
			return '\b';

		case 'n':
			return '\n';

		case 'r':
			return '\r';

		case 'f':
			return '\f';

		case '\n':
			lineNumber++;
			lineStart = pos;
			// fall-through

		case '\'':
		case '"':
		case '\\':
		case '/':
			return escaped;
		default:
			// throw error when none of the above cases are matched
			throw new RuntimeException("Invalid escape sequence");
		}

	}

	private void skipUnquotedValue() {
		do {
			int i = 0;
			for (; pos + i < limit; i++) {
				switch (buffer[pos + i]) {
				case '/':
				case '\\':
				case ';':
				case '#':
				case '=':
					checkLenient(); // fall-through
				case '{':
				case '}':
				case '[':
				case ']':
				case ':':
				case ',':
				case ' ':
				case '\t':
				case '\f':
				case '\r':
				case '\n':
					pos += i;
					return;
				}
			}
			pos += i;
		} while (fillBuffer(1));
	}

	private void push(int newTop) {
		if (stackSize == stack.length) {
			int[] newStack = new int[stackSize * 2];
			int[] newPathIndices = new int[stackSize * 2];
			String[] newPathNames = new String[stackSize * 2];
			System.arraycopy(stack, 0, newStack, 0, stackSize);
			System.arraycopy(pathIndices, 0, newPathIndices, 0, stackSize);
			System.arraycopy(pathNames, 0, newPathNames, 0, stackSize);
			stack = newStack;
			pathIndices = newPathIndices;
			pathNames = newPathNames;
		}
		stack[stackSize++] = newTop;
	}

	public String nextName() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		String result;
		if (p == PEEKED_UNQUOTED_NAME) {
			result = nextUnquotedValue();
		} else if (p == PEEKED_SINGLE_QUOTED_NAME) {
			result = nextQuotedValue('\'');
		} else if (p == PEEKED_DOUBLE_QUOTED_NAME) {
			result = nextQuotedValue('"');
		} else {
			throw new IllegalStateException("Expected a name but was " + peek()
					+ locationString());
		}
		peeked = PEEKED_NONE;
		pathNames[stackSize - 1] = result;
		return result;
	}

	public String nextString() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		String result;
		if (p == PEEKED_UNQUOTED) {
			result = nextUnquotedValue();
		} else if (p == PEEKED_SINGLE_QUOTED) {
			result = nextQuotedValue('\'');
		} else if (p == PEEKED_DOUBLE_QUOTED) {
			result = nextQuotedValue('"');
		} else if (p == PEEKED_BUFFERED) {
			result = peekedString;
			peekedString = null;
		} else if (p == PEEKED_LONG) {
			result = Long.toString(peekedLong);
		} else if (p == PEEKED_NUMBER) {
			result = new String(buffer, pos, peekedNumberLength);
			pos += peekedNumberLength;
		} else {
			throw new IllegalStateException("Expected a string but was "
					+ peek() + locationString());
		}
		peeked = PEEKED_NONE;
		pathIndices[stackSize - 1]++;
		return result;
	}

	public Boolean nextBoolean() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		if (p == PEEKED_TRUE) {
			peeked = PEEKED_NONE;
			pathIndices[stackSize - 1]++;
			return true;
		} else if (p == PEEKED_FALSE) {
			peeked = PEEKED_NONE;
			pathIndices[stackSize - 1]++;
			return false;
		}
		throw new IllegalStateException("Expected a boolean but was " + peek()
				+ locationString());
	}

	public void nextNull() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}
		if (p == PEEKED_NULL) {
			peeked = PEEKED_NONE;
			pathIndices[stackSize - 1]++;
		} else {
			throw new IllegalStateException("Expected null but was " + peek()
					+ locationString());
		}
	}

	public double nextDouble() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}

		if (p == PEEKED_LONG) {
			peeked = PEEKED_NONE;
			pathIndices[stackSize - 1]++;
			return (double) peekedLong;
		}

		if (p == PEEKED_NUMBER) {
			peekedString = new String(buffer, pos, peekedNumberLength);
			pos += peekedNumberLength;
		} else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED) {
			peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\''
					: '"');
		} else if (p == PEEKED_UNQUOTED) {
			peekedString = nextUnquotedValue();
		} else if (p != PEEKED_BUFFERED) {
			throw new IllegalStateException("Expected a double but was "
					+ peek() + locationString());
		}

		peeked = PEEKED_BUFFERED;
		double result = Double.parseDouble(peekedString); // don't catch this
															// NumberFormatException.
		if (!lenient && (Double.isNaN(result) || Double.isInfinite(result))) {
			throw new RuntimeException("JSON forbids NaN and infinities: "
					+ result + locationString());
		}
		peekedString = null;
		peeked = PEEKED_NONE;
		pathIndices[stackSize - 1]++;
		return result;
	}

	public long nextLong() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}

		if (p == PEEKED_LONG) {
			peeked = PEEKED_NONE;
			pathIndices[stackSize - 1]++;
			return peekedLong;
		}

		if (p == PEEKED_NUMBER) {
			peekedString = new String(buffer, pos, peekedNumberLength);
			pos += peekedNumberLength;
		} else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED
				|| p == PEEKED_UNQUOTED) {
			if (p == PEEKED_UNQUOTED) {
				peekedString = nextUnquotedValue();
			} else {
				peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\''
						: '"');
			}
			try {
				long result = Long.parseLong(peekedString);
				peeked = PEEKED_NONE;
				pathIndices[stackSize - 1]++;
				return result;
			} catch (NumberFormatException ignored) {
				// Fall back to parse as a double below.
			}
		} else {
			throw new IllegalStateException("Expected a long but was " + peek()
					+ locationString());
		}

		peeked = PEEKED_BUFFERED;
		double asDouble = Double.parseDouble(peekedString); // don't catch this
															// NumberFormatException.
		long result = (long) asDouble;
		if (result != asDouble) { // Make sure no precision was lost casting to
									// 'long'.
			throw new NumberFormatException("Expected a long but was "
					+ peekedString + locationString());
		}
		peekedString = null;
		peeked = PEEKED_NONE;
		pathIndices[stackSize - 1]++;
		return result;
	}

	public int nextInt() {
		int p = peeked;
		if (p == PEEKED_NONE) {
			p = doPeek();
		}

		int result;
		if (p == PEEKED_LONG) {
			result = (int) peekedLong;
			if (peekedLong != result) { // Make sure no precision was lost
										// casting to 'int'.
				throw new NumberFormatException("Expected an int but was "
						+ peekedLong + locationString());
			}
			peeked = PEEKED_NONE;
			pathIndices[stackSize - 1]++;
			return result;
		}

		if (p == PEEKED_NUMBER) {
			peekedString = new String(buffer, pos, peekedNumberLength);
			pos += peekedNumberLength;
		} else if (p == PEEKED_SINGLE_QUOTED || p == PEEKED_DOUBLE_QUOTED
				|| p == PEEKED_UNQUOTED) {
			if (p == PEEKED_UNQUOTED) {
				peekedString = nextUnquotedValue();
			} else {
				peekedString = nextQuotedValue(p == PEEKED_SINGLE_QUOTED ? '\''
						: '"');
			}
			try {
				result = Integer.parseInt(peekedString);
				peeked = PEEKED_NONE;
				pathIndices[stackSize - 1]++;
				return result;
			} catch (NumberFormatException ignored) {
				// Fall back to parse as a double below.
			}
		} else {
			throw new IllegalStateException("Expected an int but was " + peek()
					+ locationString());
		}

		peeked = PEEKED_BUFFERED;
		double asDouble = Double.parseDouble(peekedString); // don't catch this
															// NumberFormatException.
		result = (int) asDouble;
		if (result != asDouble) { // Make sure no precision was lost casting to
									// 'int'.
			throw new NumberFormatException("Expected an int but was "
					+ peekedString + locationString());
		}
		peekedString = null;
		peeked = PEEKED_NONE;
		pathIndices[stackSize - 1]++;
		return result;
	}

	public void skipValue() {
		int count = 0;
		do {
			int p = peeked;
			if (p == PEEKED_NONE) {
				p = doPeek();
			}

			if (p == PEEKED_BEGIN_ARRAY) {
				push(JsonScope.EMPTY_ARRAY);
				count++;
			} else if (p == PEEKED_BEGIN_OBJECT) {
				push(JsonScope.EMPTY_OBJECT);
				count++;
			} else if (p == PEEKED_END_ARRAY) {
				stackSize--;
				count--;
			} else if (p == PEEKED_END_OBJECT) {
				stackSize--;
				count--;
			} else if (p == PEEKED_UNQUOTED_NAME || p == PEEKED_UNQUOTED) {
				skipUnquotedValue();
			} else if (p == PEEKED_SINGLE_QUOTED
					|| p == PEEKED_SINGLE_QUOTED_NAME) {
				skipQuotedValue('\'');
			} else if (p == PEEKED_DOUBLE_QUOTED
					|| p == PEEKED_DOUBLE_QUOTED_NAME) {
				skipQuotedValue('"');
			} else if (p == PEEKED_NUMBER) {
				pos += peekedNumberLength;
			}
			peeked = PEEKED_NONE;
		} while (count != 0);

		pathIndices[stackSize - 1]++;
		pathNames[stackSize - 1] = "null";
	}
}
