package fish.json.adapter;

import fish.json.base.Gson;

public interface TypeAdapterFactory {
	<T> TypeAdapter<T> create(Gson gson, Class<?> type);
}
