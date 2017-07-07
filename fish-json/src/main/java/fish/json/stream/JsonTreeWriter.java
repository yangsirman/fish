package fish.json.stream;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import fish.json.entity.JsonArray;
import fish.json.entity.JsonElement;
import fish.json.entity.JsonNull;
import fish.json.entity.JsonObject;
import fish.json.entity.JsonPrimitive;

public class JsonTreeWriter extends JsonWriter {
	private static Writer unableWriter = new Writer() {

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			throw new AssertionError();
		}

		@Override
		public void flush() throws IOException {
			throw new AssertionError();
		}

		@Override
		public void close() throws IOException {
			throw new AssertionError();
		}

	};
	
	private static final JsonPrimitive SENTINEL_CLOSED = new JsonPrimitive("closed");
	
	private JsonElement product = JsonNull.INSTANCE;

	private String pendingName;

	private List<JsonElement> stack = new ArrayList<JsonElement>();

	public JsonTreeWriter() {
		super(unableWriter);
	}

	public JsonElement get() {
		if (!this.stack.isEmpty()) {
			throw new RuntimeException("Expected one JSON element but was "
					+ stack);
		}
		return product;
	}

	private JsonElement peek() {
		return this.stack.get(this.stack.size() - 1);
	}

	private void put(JsonElement value) {
		if (this.pendingName != null) {
			if (this.stack.isEmpty() || !value.isObject()) {
				throw new RuntimeException();
			}
			JsonObject obj = (JsonObject) peek();
			obj.add(this.pendingName, value);
			this.pendingName = null;
		} else if (this.stack.isEmpty()) {
			this.product = value;
		} else {
			JsonArray obj = (JsonArray) peek();
			obj.add(value);
		}
	}

	@Override
	public JsonWriter beginArray() {
		JsonArray array = new JsonArray();
		put(array);
		this.stack.add(array);
		return this;
	}

	@Override
	public JsonWriter endArray() {
		if (this.stack.isEmpty() || this.pendingName != null) {
			throw new IllegalStateException();
		}
		JsonElement ele = peek();
		if (ele.isArray()) {
			this.stack.remove(this.stack.size() - 1);
			return this;
		}
		throw new IllegalStateException();
	}

	@Override
	public JsonWriter beginObject() {
		JsonObject o = new JsonObject();
		this.put(o);
		this.stack.add(o);
		return this;
	}

	@Override
	public JsonWriter endObject() {
		if (this.stack.isEmpty() || this.pendingName != null) {
			throw new IllegalStateException();
		}
		JsonElement ele = peek();
		if (ele.isObject()) {
			this.stack.remove(this.stack.size() - 1);
			return this;
		}
		throw new IllegalStateException();
	}

	@Override
	public JsonWriter name(String name) {
		if (this.stack.isEmpty() || this.pendingName != null) {
			throw new IllegalStateException();
		}
		JsonElement ele = peek();
		if (ele.isObject()) {
			this.pendingName = name;
			return this;
		}
		throw new IllegalStateException();
	}

	public JsonWriter nullValue() {
		this.put(JsonNull.INSTANCE);
		return this;
	}

	@Override
	public JsonWriter value(Number value) {
		if (value == null)
			return this.nullValue();
		this.put(new JsonPrimitive(value));
		return this;
	}

	@Override
	public JsonWriter value(String value) {
		if (value == null)
			return this.nullValue();
		this.put(new JsonPrimitive(value));
		return this;
	}

	@Override
	public JsonWriter value(Boolean value) {
		if (value == null)
			return this.nullValue();
		this.put(new JsonPrimitive(value));
		return this;
	}
	
	@Override
	public void flush() {

	}

	@Override
	public void close() {
		if (!stack.isEmpty()) {
			throw new JsonIOException("Incomplete document");
		}
		stack.add(SENTINEL_CLOSED);
	}
}
