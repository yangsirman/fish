package fish.json.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeToken<T> {
	final Class<? super T> rawType;
	final Type type;
	final int hashCode;

	@SuppressWarnings("unchecked")
	protected TypeToken() {
		this.type = getSuperclassTypeParameter(getClass());
		this.rawType = (Class<? super T>) $Gson$Types.getRawType(type);
		this.hashCode = type.hashCode();
	}

	static Type getSuperclassTypeParameter(Class<?> subclass) {
		Type superclass = subclass.getGenericSuperclass();
		if (superclass instanceof Class) {
			throw new RuntimeException("Missing type parameter.");
		}
		ParameterizedType parameterized = (ParameterizedType) superclass;
		return $Gson$Types
				.canonicalize(parameterized.getActualTypeArguments()[0]);
	}

	public final Class<? super T> getRawType() {
		return rawType;
	}

	public final Type getType() {
		return type;
	}
}
