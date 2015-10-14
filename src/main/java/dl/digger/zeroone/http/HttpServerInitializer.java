package dl.digger.zeroone.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HttpServerInitializer {
	private Map<String, Object> initMap = new ConcurrentHashMap<String, Object>();

	public void init() {
		initMap.put("ServerName", "zero-one-http-server/1.0");
		init0();
	}

	public Map<String, Object> getInitMap() {
		return initMap;
	}

	public abstract void init0();
}
