package dl.digger.zeroone.http.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ZeroOneUtil {
	

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
	
	public static Map<String,Object> getResultMap(int rtn, Object data, String msg){
		Map<String,Object> result = new HashMap<String, Object>();
		result.put("rtn", rtn);
		if(data!=null){
			result.put("data", data);
		}
		if(msg!=null){
			result.put("msg", msg);
		}
		return result;
	}
	
	
}
