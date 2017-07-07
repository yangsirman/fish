package fish.json.entity;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonPrimitive extends JsonElement {

	private static final Class<?>[] PRIMITIVE_TYPES = { int.class,
			boolean.class, long.class, double.class, float.class, char.class,
			byte.class, short.class, Integer.class, Long.class, Double.class,
			Float.class, Character.class, Byte.class, Boolean.class,
			Short.class };

	private Object value;

	public JsonPrimitive(Boolean b) {
		this.setValue(b);
	}

	public JsonPrimitive(Number n) {
		this.setValue(n);
	}

	public JsonPrimitive(String s) {
		this.setValue(s);
	}

	public JsonPrimitive(Character c) {
		this.setValue(c);
	}

	private void setValue(Object o) {
		if (o instanceof Character) {
			this.value = String.valueOf(((Character) o).charValue());
		} else {
			this.value = o;
		}
	}

	@Override
	public JsonElement deepCopy() {
		return this;
	}

	public JsonPrimitive(Object value) {
		if (!isPrimitive(value)) {
			throw new RuntimeException("value不是基元类型");
		}
		this.value = value;
	}

	public boolean isString() {
		return this.value instanceof String;
	}

	@Override
	public String getAsString() {
		if (isNumber()) {
			return getAsNumber().toString();
		} else if (isBoolean()) {
			return this.getBooleanWrapper().toString();
		} else {
			return (String) value;
		}
	}
	
	@Override
	public Number getAsNumber() {
		return (Number) value;
	}

	public boolean isNumber() {
		return this.value instanceof Number;
	}

	public boolean isBoolean() {
		return this.value instanceof Boolean;
	}

	private Boolean getBooleanWrapper() {
		return (Boolean) this.value;
	}

	@Override
	public boolean getAsBoolean() {
		if (this.isBoolean()) {
			return this.getBooleanWrapper().booleanValue();
		} else {
			return false;
		}
	}

	@Override
	public double getAsDouble() {
		return isNumber() ? this.getAsNumber().doubleValue() : Double
				.parseDouble(this.getAsString());
	}

	@Override
	public float getAsFloat() {
		return isNumber() ? this.getAsNumber().floatValue() : Float
				.parseFloat(this.getAsString());
	}

	@Override
	public int getAsInteger() {
		return isNumber() ? this.getAsNumber().intValue() : Integer
				.parseInt(this.getAsString());
	}

	@Override
	public long getAsLong() {
		return isNumber() ? this.getAsNumber().longValue() : Long
				.parseLong(this.getAsString());
	}

	@Override
	public BigDecimal getAsBigDecimal() {
		return value instanceof BigDecimal ? (BigDecimal) value
				: new BigDecimal(value.toString());
	}

	@Override
	public BigInteger getAsBigInteger() {
		return value instanceof BigInteger ? (BigInteger) value
				: new BigInteger(value.toString());
	}

	@Override
	public byte getAsByte() {
		return isNumber() ? getAsNumber().byteValue() : Byte
				.parseByte(getAsString());
	}

	@Override
	public char getAsCharacter() {
		return getAsString().charAt(0);
	}

	private static boolean isPrimitive(Object target) {
		if (target instanceof String)
			return true;

		Class<?> classOfPrimitive = target.getClass();
		for (Class<?> standardPrimitive : PRIMITIVE_TYPES) {
			if (standardPrimitive.isAssignableFrom(classOfPrimitive)) {
				return true;
			}
		}

		return false;
	}
}
