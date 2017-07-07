package fish.json.base;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import fish.json.adapter.TypeAdapter;
import fish.json.adapter.TypeAdapterFactory;
import fish.json.entity.JsonElement;
import fish.json.stream.JsonReader;
import fish.json.stream.JsonTreeReader;
import fish.json.stream.JsonTreeWriter;
import fish.json.stream.JsonWriter;

public class Gson {
	private GsonBuilder builder;

	public GsonBuilder getBuilder() {
		return builder;
	}

	private boolean htmlSafe = false;
	private boolean leninet = false;
	private boolean serializeNulls = false;
	private String datePattern = "yyyy/MM/dd";

	private List<TypeAdapterFactory> factories;
	@SuppressWarnings("unused")
	private Map<Integer, TypeAdapter<?>> binders;

	public Gson(GsonBuilder builder, boolean hs, boolean leninet,
			boolean snull, String pattern, List<TypeAdapterFactory> factories,
			Map<Integer, TypeAdapter<?>> defaultFactories) {
		this.builder = builder;
		this.htmlSafe = hs;
		this.leninet = leninet;
		this.serializeNulls = snull;
		this.datePattern = pattern;
		this.factories = factories;
		this.binders = defaultFactories;
	}

	public boolean isHtmlSafe() {
		return htmlSafe;
	}

	public boolean isLeninet() {
		return leninet;
	}

	public boolean serializeNulls() {
		return serializeNulls;
	}

	public String datePattern() {
		return datePattern;
	}

	public JsonElement toTreeJson(Object src) {
		JsonTreeWriter writer = new JsonTreeWriter();
		this.toJson(src, src.getClass(), writer);
		return writer.get();
	}

	public String toJson(Object src) {
		StringWriter writer = new StringWriter();
		this.toJson(src, writer);
		return writer.toString();
	}

	public void toJson(Object src, Writer writer) {
		
		JsonWriter out = new JsonWriter(writer);
		if (src == null) {
			this.toJson(src,null,out);
			return;
		}
		this.toJson(src, src.getClass(), out);
	}

	public void toJson(Object src, Class<?> typeOfSrc, JsonWriter writer) {
		TypeAdapter<?> adapter = null;
		for (TypeAdapterFactory factory : this.factories) {
			adapter = factory.create(this, typeOfSrc);
			if (adapter != null)
				break;
		}
		if (adapter == null) {
			throw new RuntimeException("缺少对应的adapter");
		}
		adapter.write(writer, src);
	}
	
	public Object objectFromJson(JsonReader reader, Class<?> clazz) {
	    reader.setLenient(true);
	    TypeAdapter<?> adapter = null;
		for (TypeAdapterFactory factory : this.factories) {
			adapter = factory.create(this, clazz);
			if (adapter != null)
				break;
		}
		if (adapter == null) {
			throw new RuntimeException("缺少对应的adapter");
		}
		
		return adapter.read(reader);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T fromJson(JsonReader reader, Class<?> clazz) {
		return (T)objectFromJson(reader,clazz);
	}
	
	public <T> T fromJson(String json, Class<?> clazz) {
		return fromJson(new JsonReader(new StringReader(json)),clazz);
	}
	
	public <T> T fromJson(JsonElement element, Class<?> clazz) {
		return fromJson(new JsonTreeReader(element),clazz);
	}
}
