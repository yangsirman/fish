package fish.json.adapter;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class BooleanAdapter extends TypeAdapter<Boolean> {

	@Override
	public void write(JsonWriter writer, Object src) {
		if (src instanceof Boolean) {
			writer.value((Boolean) src);
			return;
		}
		throw new IllegalStateException();
	}

	@Override
	public Boolean read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		} else if (in.peek() == JsonToken.STRING) {
			return Boolean.parseBoolean(in.nextString());
		}
		return in.nextBoolean();
	}

}
