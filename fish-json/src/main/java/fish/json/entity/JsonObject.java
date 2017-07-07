package fish.json.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JsonObject extends JsonElement {
	private final Map<String, JsonElement> members = new HashMap<String, JsonElement>();

	@Override
	public JsonElement deepCopy() {
		return null;
	}

	public void add(String property, JsonElement value) {
		if (value == null) {
			value = JsonNull.INSTANCE;
		}
		this.members.put(property, value);
	}

	public JsonElement remove(String property) {
		return this.members.remove(property);
	}

	private JsonElement createElement(Object o) {
		return o == null ? JsonNull.INSTANCE : new JsonPrimitive(o);
	}

	public void addProperty(String property, String value) {
		this.add(property, createElement(value));
	}

	public void addProperty(String property, Number value) {
		this.add(property, createElement(value));
	}

	public void addProperty(String property, Boolean value) {
		this.add(property, createElement(value));
	}

	public void addProperty(String property, Character value) {
		this.add(property, createElement(value));
	}

	public Set<Map.Entry<String, JsonElement>> entrySet() {
		return this.members.entrySet();
	}

	public Set<String> keySet() {
		return this.members.keySet();
	}

	public int size() {
		return this.members.size();
	}

	public boolean has(String memberName) {
		return this.members.containsKey(memberName);
	}

	public JsonElement get(String memberName) {
		return this.members.get(memberName);
	}

	public JsonPrimitive getAsPrimitive(String memberName) {
		return (JsonPrimitive) this.members.get(memberName);
	}

	public JsonArray getAsArray(String memberName) {
		return (JsonArray) this.members.get(memberName);
	}

	public JsonObject getAsObject(String memberName) {
		return (JsonObject) this.members.get(memberName);
	}

	@Override
	public boolean equals(Object o) {
		return (o == this)|| (o instanceof JsonObject && ((JsonObject) o).members.equals(members));
	}

	@Override
	public int hashCode() {
		return this.members.hashCode();
	}
}
