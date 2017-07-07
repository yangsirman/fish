package fish.json.adapter;

import java.lang.reflect.Field;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class ObjectAdapter extends TypeAdapter<Object> {

	@Override
	public void write(JsonWriter writer, Object src) {
		if (src == null) {
			writer.nullValue();
			return;
		}

		writer.beginObject();
		Field[] fields = src.getClass().getFields();
		for (Field field : fields) {
			writer.name(field.getName());
			try {
				writer.value(field.get(src).toString());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		writer.endObject();
	}

	@Override
	public Object read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		Object instance = null;

		try {
			in.beginObject();
			while (in.hasNext()) {
				String name = in.nextName();
				Field field;
				field = instance.getClass().getField(name);
				if (field == null) {
					in.skipValue();
				} else {
					try {
						field.set(instance, in.nextString());
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (NoSuchFieldException e) {

		} catch (SecurityException e) {
			throw new AssertionError(e);
		}
		in.endObject();
		return instance;
	}

}
