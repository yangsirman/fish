package fish.json.entity;

import java.math.BigDecimal;
import java.math.BigInteger;

public abstract class JsonElement {

	public abstract JsonElement deepCopy();

	public boolean isArray() {
		return this instanceof JsonArray;
	}

	public boolean isObject() {
		return this instanceof JsonObject;
	}

	public boolean isPrimitive() {
		return this instanceof JsonPrimitive;
	}

	public boolean isNull() {
		return this instanceof JsonNull;
	}

	public JsonObject getAsJsonObject() {
		if (!this.isObject()) {
			throw new IllegalStateException("Not a JSON Object: " + this);
		}
		return (JsonObject) this;
	}

	public JsonArray getAsJsonArray() {
		if (!this.isObject()) {
			throw new IllegalStateException("Not a JSON Array: " + this);
		}
		return (JsonArray) this;
	}

	public JsonPrimitive getAsPrimitive() {
		if (!this.isPrimitive()) {
			throw new IllegalStateException("Not a JSON Primitive: " + this);
		}
		return (JsonPrimitive) this;
	}

	public JsonNull getAsJsonNull() {
		if (!this.isNull()) {
			throw new IllegalStateException("Not a JSON Null: " + this);
		}
		return (JsonNull) this;
	}

	public boolean getAsBoolean() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public int getAsInteger() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public long getAsLong() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public double getAsDouble() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public float getAsFloat() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public short getAsShort() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public char getAsCharacter() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public byte getAsByte() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public String getAsString() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}
	
	public Number getAsNumber() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}
	
	public BigDecimal getAsBigDecimal() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}

	public BigInteger getAsBigInteger() {
		throw new UnsupportedOperationException(getClass().getSimpleName());
	}
}
