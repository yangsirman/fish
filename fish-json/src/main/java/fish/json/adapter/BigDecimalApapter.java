package fish.json.adapter;

import java.math.BigDecimal;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class BigDecimalApapter extends TypeAdapter<BigDecimal> {

	@Override
	public void write(JsonWriter writer, Object src) {
		writer.value(src.toString());
	}

	@Override
	public BigDecimal read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return new BigDecimal(in.nextString());
	}

}
