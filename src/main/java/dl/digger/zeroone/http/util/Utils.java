package dl.digger.zeroone.http.util;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
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

}
