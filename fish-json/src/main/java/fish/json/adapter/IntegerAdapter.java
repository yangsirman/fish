package fish.json.adapter;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class IntegerAdapter extends TypeAdapter<Integer> {

	@Override
	public void write(JsonWriter writer, Object src) {
		writer.value((Number)src);
	}

	@Override
	public Integer read(JsonReader in) {
		if(in.peek()==JsonToken.NULL){
			in.nextNull();
			return null;
		}
		return in.nextInt();
	}

}
