package dl.digger.zeroone.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.util.StringUtils;

import dl.digger.zeroone.http.exception.CmdException;
import dl.digger.zeroone.http.util.ErrorCode;

public class ZeroOneHttpRequest {

	final Logger logger = LoggerFactory.getLogger(getClass());
	private Map<String, List<String>> params;
	private String characterEncoding;
	private HttpRequest request;
	private String uri;
	private String path;
	private Map<String, List<Cookie>> cookies;

	public ZeroOneHttpRequest(HttpRequest request) throws IOException {
		this.request = request;
		this.characterEncoding = getContentType();
		if (StringUtils.isEmpty(this.characterEncoding)) {
			this.characterEncoding = "UTF-8";
		}
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
				request.getUri(), Charset.forName(this.characterEncoding));
		this.path = queryStringDecoder.path();
		this.uri = queryStringDecoder.uri();
		if (request.getMethod().equals(HttpMethod.GET)) {
			this.params = queryStringDecoder.parameters();
		} else if (request.getMethod().equals(HttpMethod.POST)) {
			HttpPostRequestDecoder httpPostRequestDecoder = new HttpPostRequestDecoder(
					new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE),
					request, Charset.forName(this.characterEncoding));
			this.params = initParametersByPost(httpPostRequestDecoder);
		} else {
			this.params = Collections.EMPTY_MAP;
		}
		initCookies();
	}

	private void initCookies() {
		this.cookies = new HashMap<String, List<Cookie>>();
		String cookieString = HttpHeaders.getHeader(this.request,
				HttpHeaders.Names.COOKIE);
		if (cookieString != null) {
			Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
			if (!cookies.isEmpty()) {
				for (Cookie cookie : cookies) {
					List<Cookie> list = this.cookies.get(cookie.name());
					if(list==null){
						list = new ArrayList<Cookie>();
						this.cookies.put(cookie.name(), list);
					}
					list.add(cookie);
				}
			}
		}
	}

	private Map<String, List<String>> initParametersByPost(
			HttpPostRequestDecoder httpPostRequestDecoder) throws IOException {
		Map<String, List<String>> params = new HashMap<String, List<String>>();
		if (httpPostRequestDecoder == null) {
			return params;
		}
		List<InterfaceHttpData> datas = httpPostRequestDecoder
				.getBodyHttpDatas();
		if (datas != null) {
			for (InterfaceHttpData data : datas) {
				if (data instanceof Attribute) {
					Attribute attribute = (Attribute) data;
					String key = attribute.getName();
					String value = attribute.getValue();

					List<String> ori = params.get(key);
					if (ori == null) {
						ori = new ArrayList<String>(1);
						params.put(key, ori);
					}
					ori.add(value);
				}
			}
		}
		return params;
	}

	public String getReferer() {
		return HttpHeaders.getHeader(this.request, HttpHeaders.Names.REFERER);
	}

	public Map<String, List<String>> getAllParams() {
		return this.params;
	}

	public String getUri() {
		return uri;
	}

	public String getReqId() {
		String uri = this.path;
		if (uri.length() > 2 && uri.charAt(uri.length() - 1) == '/') {
			uri = uri.substring(0, uri.length() - 1);
		}
		return uri;
	}

	public String getProtocolVersion() {
		return this.request.getProtocolVersion().text();
	}

	public String getMethod() {
		return this.getMethodObject().name();
	}

	public HttpMethod getMethodObject() {
		return this.request.getMethod();
	}

	public String getStringParam(String key) {
		return getStringParam(key, null);
	}

	public String getStringParam(String key, String defaultValue) {
		List<String> lists = this.params.get(key);
		if (lists != null && lists.size() > 0) {
			return lists.get(0);
		}
		return defaultValue;
	}

	public int getIntParam(String key) throws CmdException {
		return getIntParam(key, 0);
	}

	public int getIntParam(String key, int defaultValue) throws CmdException {
		String param = getStringParam(key, "0");
		if (param == null) {
			return defaultValue;
		}
		int param_int = defaultValue;
		try {
			param_int = Integer.valueOf(param);
		} catch (NumberFormatException e) {
			throw new CmdException(ErrorCode.CMD_PARAM_ERROR, key + " error");
		}
		return param_int;
	}

	public Integer getIntegerParam(String key) throws CmdException {
		return getIntegerParam(key, null);
	}

	public Integer getIntegerParam(String key, Integer defaultValue)
			throws CmdException {
		String defaultValue_str = defaultValue == null ? null : defaultValue
				.toString();
		String param = getStringParam(key, defaultValue_str);
		if (param == null) {
			return defaultValue;
		}
		Integer param_int = defaultValue;
		try {
			param_int = Integer.valueOf(param);
		} catch (NumberFormatException e) {
			throw new CmdException(ErrorCode.CMD_PARAM_ERROR, key + " error");
		}
		return param_int;
	}

	public String getContentType() {
		return HttpHeaders.getHeader(this.request,
				HttpHeaders.Names.CONTENT_TYPE);
	}


	public Map<String, List<Cookie>> getCookies() {
		return this.cookies;
	}

	public List<Cookie> getCookie(String name){
		return this.cookies.get(name);
	}
	
	public HttpRequest getRequest() {
		return request;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
