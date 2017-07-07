package fish.json.adapter;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class ShortAdapter extends TypeAdapter<Short> {

	@Override
	public void write(JsonWriter writer, Object src) {
		writer.value((Number)src);
	}

	@Override
	public Short read(JsonReader in) {
		if(in.peek()==JsonToken.NULL){
			in.nextNull();
			return null;
		}
		return Integer.valueOf(in.nextInt()).shortValue();
	}

}
