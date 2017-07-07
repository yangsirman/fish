package fish.json.adapter;

import java.io.*;

import fish.json.entity.JsonElement;
import fish.json.stream.JsonReader;
import fish.json.stream.JsonTreeReader;
import fish.json.stream.JsonTreeWriter;
import fish.json.stream.JsonWriter;

public abstract class TypeAdapter<T> {
	public abstract void write(JsonWriter writer, Object src);
	
	public final void writeCast(JsonWriter writer,T src){
		this.write(writer, src);
	}
	
	public final void toJson(Writer out, T value) {
		JsonWriter w = new JsonWriter(out);
		this.write(w, value);
	}

	public final String toJson(T value) {
		StringWriter out = new StringWriter();
		this.toJson(out, value);
		return out.toString();
	}

	public final JsonElement toJsonTree(T value) {
		JsonTreeWriter writer = new JsonTreeWriter();
		this.write(writer, value);
		return writer.get();
	}

	public abstract T read(JsonReader in);

	public final T fromJson(Reader in) {
		JsonReader reader = new JsonReader(in);
		return this.read(reader);
	}

	public final T fromJson(String json) {
		return this.fromJson(new StringReader(json));
	}

	public final T fromJsonTree(JsonElement jsonTree) {
		JsonTreeReader jsonReader = new JsonTreeReader(jsonTree);
		return read(jsonReader);
	}

}
