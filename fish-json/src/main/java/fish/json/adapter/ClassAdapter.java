package fish.json.adapter;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonWriter;

@SuppressWarnings("rawtypes")
public class ClassAdapter extends TypeAdapter<Class> {

	@Override
	public void write(JsonWriter writer, Object src) {
		throw new UnsupportedOperationException("Attempted to serialize java.lang.Class: "
	              + ((Class)src).getName() + ". Forgot to register a type adapter?");
	}

	@Override
	public Class read(JsonReader in) {
		throw new UnsupportedOperationException(
	              "Attempted to deserialize a java.lang.Class. Forgot to register a type adapter?");
	}
}
