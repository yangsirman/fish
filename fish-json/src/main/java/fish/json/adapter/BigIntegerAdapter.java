package fish.json.adapter;

import java.math.BigInteger;

import fish.json.stream.JsonReader;
import fish.json.stream.JsonToken;
import fish.json.stream.JsonWriter;

public class BigIntegerAdapter extends TypeAdapter<BigInteger> {

	@Override
	public void write(JsonWriter writer, Object src) {
		writer.value((BigInteger)src);
		
	}

	@Override
	public BigInteger read(JsonReader in) {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return new BigInteger(in.nextString());
	}

}
