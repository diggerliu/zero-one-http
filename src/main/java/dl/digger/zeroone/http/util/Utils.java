package dl.digger.zeroone.http.util;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Utils {

	public static final DefaultFullHttpResponse NOT_FOUND_RESP = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
	public static final DefaultFullHttpResponse INTERNAL_SERVER_ERROR_RESP = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
	public static final DefaultFullHttpResponse NOT_IMPLEMENTED_RESP = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_IMPLEMENTED);
	public final byte[] EMPTY_BYTE = new byte[0];

	public static String DEFAULT_CHARACTERENCODING = "UTF-8";
	public static Charset DEFAULT_CHARSET = Charset
			.forName(DEFAULT_CHARACTERENCODING);

	public static String resolveNicAddr(String nic) {
		try {
			NetworkInterface ni = NetworkInterface.getByName(nic);
			Enumeration<InetAddress> addrs = ni.getInetAddresses();
			while (addrs.hasMoreElements()) {
				InetAddress i = addrs.nextElement();
				if (i instanceof Inet4Address) {
					return i.getHostAddress();
				}
			}
			addrs = ni.getInetAddresses();
			return addrs.nextElement().getHostAddress();
		} catch (Exception e) {
			return null;
		}
	}

	public static Map<String, Object> getResultMap(int rtn, Object data,
			String msg) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("rtn", rtn);
		if (data != null) {
			result.put("data", data);
		}
		if (msg != null) {
			result.put("msg", msg);
		}
		return result;
	}

	public static final String getCharsetFromContentType(String contentType) {

		if (contentType == null) {
			return (null);
		}
		int start = contentType.indexOf("charset=");
		if (start < 0) {
			return (null);
		}
		String encoding = contentType.substring(start + 8);
		int end = encoding.indexOf(';');
		if (end >= 0) {
			encoding = encoding.substring(0, end);
		}
		encoding = encoding.trim();
		if ((encoding.length() > 2) && (encoding.startsWith("\""))
				&& (encoding.endsWith("\""))) {
			encoding = encoding.substring(1, encoding.length() - 1);
		}
		return (encoding.trim());

	}
}
