package fish.json.stream;

import java.io.IOException;

public class JsonIOException extends JsonException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1456052579131809724L;
	
	private IOException innerException;
	
	public JsonIOException() {
		// TODO 自动生成的构造函数存根
	}
	
	public JsonIOException(IOException innerException) {
		this.innerException=innerException;
	}
	
	public JsonIOException(String message) {
		super(message);
		// TODO 自动生成的构造函数存根
	}

	public JsonIOException(Throwable cause) {
		super(cause);
		// TODO 自动生成的构造函数存根
	}

	public JsonIOException(String message, Throwable cause) {
		super(message, cause);
		// TODO 自动生成的构造函数存根
	}

	public JsonIOException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public IOException getInnerException() {
		return innerException;
	}
}
