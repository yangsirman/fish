package fish.json.entity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonArray extends JsonElement implements Iterable<JsonElement> {
	private final List<JsonElement> elements;

	public JsonArray() {
		this.elements = new ArrayList<JsonElement>();
	}

	public JsonArray(int capacity) {
		this.elements = new ArrayList<JsonElement>(capacity);
	}

	public void add(Boolean v) {
		this.elements.add(v == null ? JsonNull.INSTANCE : new JsonPrimitive(v));
	}

	public void add(Number v) {
		this.elements.add(v == null ? JsonNull.INSTANCE : new JsonPrimitive(v));
	}

	public void add(String v) {
		this.elements.add(v == null ? JsonNull.INSTANCE : new JsonPrimitive(v));
	}

	public void add(Character v) {
		this.elements.add(v == null ? JsonNull.INSTANCE : new JsonPrimitive(v));
	}

	public void add(JsonElement v) {
		this.elements.add(v == null ? JsonNull.INSTANCE : v);
	}

	public void addAll(JsonArray array) {
		this.elements.addAll(array.elements);
	}

	public JsonElement set(int index, JsonElement element) {
		return this.elements.set(index, element);
	}

	public boolean remove(JsonElement o) {
		return this.elements.remove(o);
	}

	public JsonElement remove(int i) {
		return this.elements.remove(i);
	}

	public boolean contains(JsonElement element) {
		return elements.contains(element);
	}

	public int size() {
		return this.elements.size();
	}

	public JsonElement get(int index) {
		return this.elements.get(index);
	}

	@Override
	public Number getAsNumber() {
		if (elements.size() == 1) {
			return elements.get(0).getAsNumber();
		}
		throw new IllegalStateException();
	}

	@Override
	public String getAsString() {
		if (elements.size() == 1) {
			return elements.get(0).getAsString();
		}
		throw new IllegalStateException();
	}

	@Override
	public int getAsInteger() {
		if (elements.size() == 1) {
			return elements.get(0).getAsInteger();
		}
		throw new IllegalStateException();
	}

	@Override
	public long getAsLong() {
		if (elements.size() == 1) {
			return elements.get(0).getAsLong();
		}
		throw new IllegalStateException();
	}

	@Override
	public short getAsShort() {
		if (elements.size() == 1) {
			return elements.get(0).getAsShort();
		}
		throw new IllegalStateException();
	}

	@Override
	public double getAsDouble() {
		if (elements.size() == 1) {
			return elements.get(0).getAsDouble();
		}
		throw new IllegalStateException();
	}

	@Override
	public float getAsFloat() {
		if (elements.size() == 1) {
			return elements.get(0).getAsFloat();
		}
		throw new IllegalStateException();
	}

	@Override
	public BigDecimal getAsBigDecimal() {
		if (elements.size() == 1) {
			return elements.get(0).getAsBigDecimal();
		}
		throw new IllegalStateException();
	}

	@Override
	public BigInteger getAsBigInteger() {
		if (elements.size() == 1) {
			return elements.get(0).getAsBigInteger();
		}
		throw new IllegalStateException();
	}

	@Override
	public byte getAsByte() {
		if (elements.size() == 1) {
			return elements.get(0).getAsByte();
		}
		throw new IllegalStateException();
	}

	@Override
	public char getAsCharacter() {
		if (elements.size() == 1) {
			return elements.get(0).getAsCharacter();
		}
		throw new IllegalStateException();
	}

	@Override
	public boolean getAsBoolean() {
		if (elements.size() == 1) {
			return elements.get(0).getAsBoolean();
		}
		throw new IllegalStateException();
	}

	@Override
	public boolean equals(Object o) {
		return (o == this)
				|| (o instanceof JsonArray && ((JsonArray) o).elements
						.equals(elements));
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public JsonElement deepCopy() {
		if (!elements.isEmpty()) {
			JsonArray result = new JsonArray(elements.size());
			for (JsonElement element : elements) {
				result.add(element.deepCopy());
			}
			return result;
		}
		return new JsonArray();
	}

	public Iterator<JsonElement> iterator() {
		return elements.iterator();
	}

}
