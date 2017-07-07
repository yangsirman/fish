package fish.json.adapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import fish.json.base.Gson;
import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class ArrayAdapter extends TypeAdapter<Object> {

	private Gson gson;
	private TypeAdapter<?> componentTypeAdapter;
	private Class<?> componentType;

	@Override
	public void write(JsonWriter writer, Object src) {
		if (src == null) {
			writer.nullValue();
			return;
		}
		if ("[".equals(src.getClass().getName().charAt(0))) {
			writer.beginArray();
			for (int i = 0; i < Array.getLength(src); i++) {
				Object o = Array.get(src, i);
				gson.toJson(o, o.getClass(), writer);
			}
			writer.endArray();
		}
		throw new IllegalStateException();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}

		List list = new ArrayList();
		in.beginArray();
		while (in.hasNext()) {
			Object instance = componentTypeAdapter.read(in);
			list.add(instance);
		}
		in.endArray();

		int size = list.size();
		Object array = Array.newInstance(componentType, size);
		for (int i = 0; i < size; i++) {
			Array.set(array, i, list.get(i));
		}
		return array;
	}

}
