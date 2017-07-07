package fish.json.entity;

public final class JsonNull extends JsonElement {
	
	public static JsonNull INSTANCE = new JsonNull();
	
	@Override
	public JsonElement deepCopy() {
		return INSTANCE;
	}
	
	@Override
	public int hashCode(){
		return JsonNull.class.hashCode();
	}
	
	public boolean equals(Object other){
		return this==other||other instanceof JsonNull;
	}
}
