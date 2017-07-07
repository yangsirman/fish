package fish.json.stream;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;

import fish.json.entity.*;

public class JsonTreeReader extends JsonReader {
	private static Reader unableReader = new Reader() {

		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			throw new AssertionError();
		}

		@Override
		public void close() throws IOException {
			throw new AssertionError();
		}

	};

	private static final Object SENTINEL_CLOSED = new Object();

	private String[] pathNames = new String[32];
	private int[] pathIndices = new int[32];
	private Object[] stack = new Object[32];
	private int stackSize = 0;

	private void push(Object o) {
		if (stackSize == stack.length) {
			Object[] newStack = new Object[stackSize * 2];
			int[] newPathIndices = new int[stackSize * 2];
			String[] newPathNames = new String[stackSize * 2];
			System.arraycopy(stack, 0, newStack, 0, stackSize);
			System.arraycopy(pathIndices, 0, newPathIndices, 0, stackSize);
			System.arraycopy(pathNames, 0, newPathNames, 0, stackSize);
			stack = newStack;
			pathIndices = newPathIndices;
			pathNames = newPathNames;
		}
		this.stack[this.stackSize++] = o;
	}

	private Object peekStack() {
		return this.stack[this.stackSize - 1];
	}

	private Object popStack() {
		Object result = stack[--stackSize];
		stack[stackSize] = null;
		return result;
	}

	private String locationString() {
		return " at path " + getPath();
	}

	private void expect(JsonToken expected) {
		if (peek() != expected) {
			throw new IllegalStateException("Expected " + expected
					+ " but was " + peek() + locationString());
		}
	}

	@Override
	public JsonToken peek() {
		if (this.stackSize == 0)
			return JsonToken.END_DOCUMENT;

		Object o = this.peekStack();

		if (o instanceof Iterator) {
			boolean isObject = stack[stackSize - 2] instanceof JsonObject;
			Iterator<?> iterator = (Iterator<?>) o;
			if (iterator.hasNext()) {
				if (isObject) {
					return JsonToken.NAME;
				} else {
					push(iterator.next());
					return peek();
				}
			} else {
				return isObject ? JsonToken.END_OBJECT : JsonToken.END_ARRAY;
			}
		} else if (o instanceof JsonObject) {
			return JsonToken.BEGIN_OBJECT;
		} else if (o instanceof JsonArray) {
			return JsonToken.BEGIN_ARRAY;
		} else if (o instanceof JsonPrimitive) {
			JsonPrimitive primitive = (JsonPrimitive) o;
			if (primitive.isString())
				return JsonToken.STRING;
			if (primitive.isBoolean())
				return JsonToken.BOOLEAN;
			if (primitive.isNumber()) {
				return JsonToken.NUMBER;
			}
		} else if (o == null) {
			return JsonToken.NULL;
		} else if (o == SENTINEL_CLOSED) {
			throw new IllegalStateException("JsonReader is closed");
		} else {
			throw new AssertionError();
		}
		throw new AssertionError();
	}

	public JsonTreeReader(JsonElement element) {
		super(unableReader);
		this.push(element);
	}

	@Override
	public boolean hasNext() {
		JsonToken token = this.peek();
		return token != JsonToken.END_ARRAY && token != JsonToken.END_OBJECT;
	}

	@Override
	public void beginArray() {
		expect(JsonToken.BEGIN_ARRAY);
		JsonArray array = (JsonArray) peekStack();
		push(array.iterator());
		pathIndices[stackSize - 1] = 0;
	}

	@Override
	public void endArray() {
		expect(JsonToken.END_ARRAY);
		this.popStack();
		this.popStack();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
	}

	@Override
	public void beginObject() {
		this.expect(JsonToken.BEGIN_OBJECT);
		JsonObject obj = (JsonObject) peekStack();
		this.push(obj.entrySet().iterator());
		pathIndices[stackSize - 1] = 0;
	}

	@Override
	public void endObject() {
		this.expect(JsonToken.END_OBJECT);
		this.popStack();
		this.popStack();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
	}

	@Override
	public String nextName() {
		this.expect(JsonToken.NAME);
		Iterator<?> i = (Iterator<?>) peekStack();
		Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
		String result = (String) entry.getKey();
		pathNames[stackSize - 1] = result;
		push(entry.getValue());
		return result;
	}

	@Override
	public String nextString() {
		JsonToken token = this.peek();
		if (token != JsonToken.STRING && token != JsonToken.NUMBER) {
			throw new IllegalStateException("Expected " + JsonToken.STRING
					+ " but was " + token + locationString());
		}
		String result = ((JsonPrimitive) popStack()).getAsString();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
		return result;
	}

	@Override
	public Boolean nextBoolean() {
		this.expect(JsonToken.BOOLEAN);
		Boolean result = ((JsonPrimitive) popStack()).getAsBoolean();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
		return result;
	}

	@Override
	public void nextNull() {
		expect(JsonToken.NULL);
		popStack();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
	}

	@Override
	public double nextDouble() {
		JsonToken token = peek();
		if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
			throw new IllegalStateException("Expected " + JsonToken.NUMBER
					+ " but was " + token + locationString());
		}
		double result = ((JsonPrimitive) peekStack()).getAsDouble();
		if (!isLenient() && (Double.isNaN(result) || Double.isInfinite(result))) {
			throw new NumberFormatException("JSON forbids NaN and infinities: "
					+ result);
		}
		popStack();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
		return result;
	}

	@Override
	public long nextLong() {
		JsonToken token = peek();
		if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
			throw new IllegalStateException("Expected " + JsonToken.NUMBER
					+ " but was " + token + locationString());
		}
		long result = ((JsonPrimitive) peekStack()).getAsLong();
		popStack();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
		return result;
	}

	@Override
	public int nextInt() {
		JsonToken token = peek();
		if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
			throw new IllegalStateException("Expected " + JsonToken.NUMBER
					+ " but was " + token + locationString());
		}
		int result = ((JsonPrimitive) peekStack()).getAsInteger();
		popStack();
		if (stackSize > 0) {
			pathIndices[stackSize - 1]++;
		}
		return result;
	}

	@Override
	public void close() {
		stack = new Object[] { SENTINEL_CLOSED };
		stackSize = 1;
	}

	@Override
	public String getPath() {
		StringBuilder result = new StringBuilder().append('$');
		for (int i = 0; i < stackSize; i++) {
			if (stack[i] instanceof JsonArray) {
				if (stack[++i] instanceof Iterator) {
					result.append('[').append(pathIndices[i]).append(']');
				}
			} else if (stack[i] instanceof JsonObject) {
				if (stack[++i] instanceof Iterator) {
					result.append('.');
					if (pathNames[i] != null) {
						result.append(pathNames[i]);
					}
				}
			}
		}
		return result.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public void promoteNameToValue() throws IOException {
		expect(JsonToken.NAME);
		Iterator<?> i = (Iterator<?>) peekStack();
		Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
		push(entry.getValue());
		push(new JsonPrimitive((String) entry.getKey()));
	}

}
