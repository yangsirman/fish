package fish.json.adapter;

import java.util.UUID;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class UUIDAdapter extends TypeAdapter<UUID> {

	@Override
	public void write(JsonWriter writer, Object src) {
		writer.value(src == null ? null : src.toString());
	}

	@Override
	public UUID read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return java.util.UUID.fromString(in.nextString());
	}

}
