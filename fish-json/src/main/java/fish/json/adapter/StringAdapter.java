package fish.json.adapter;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class StringAdapter extends TypeAdapter<String> {

	@Override
	public void write(JsonWriter writer, Object src) {
		if(src instanceof String||src instanceof Number||src instanceof Boolean){
			writer.value(src.toString());
			return;
		}
		throw new IllegalStateException();
	}

	@Override
	public String read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		} else if (in.peek() == JsonToken.STRING) {
			return in.nextString();
		}
		throw new IllegalStateException();
	}

}
