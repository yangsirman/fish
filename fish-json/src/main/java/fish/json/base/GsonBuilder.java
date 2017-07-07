package fish.json.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fish.json.adapter.TypeAdapter;
import fish.json.adapter.TypeAdapterFactory;

public final class GsonBuilder {
	
	private boolean htmlSafe=false;
	private boolean leninet=false;
	private boolean serializeNulls=false;
	private String datePattern="yyyy/MM/dd";
	
	public GsonBuilder htmlSafe(){
		this.htmlSafe=true;
		return this;
	}
	
	public GsonBuilder leninet(){
		this.leninet=true;
		return this;
	}
	
	public GsonBuilder serializerNulls(){
		this.serializeNulls=true;
		return this;
	}
	
	public GsonBuilder setDatePattern(String datePattern){
		this.datePattern=datePattern;
		return this;
	}
	
	private final static Map<Integer,TypeAdapter<?>> binders = new HashMap<Integer,TypeAdapter<?>>();
	
	private final static List<TypeAdapterFactory> factories = new ArrayList<TypeAdapterFactory>();
	
	public Gson create(){
		return new Gson(this,this.htmlSafe,this.leninet,this.serializeNulls,this.datePattern,factories,binders);
	}
	
	public static void registerDefaultFactory(TypeAdapter<?> typeAdapter,Class<?> type){
		binders.put(type.hashCode(),typeAdapter);
	}
	
	public static void registerFactory(TypeAdapterFactory typeAdapter,Class<?> type){
		factories.add(typeAdapter);
	}
}
