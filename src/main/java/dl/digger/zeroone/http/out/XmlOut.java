package dl.digger.zeroone.http.out;

import io.netty.handler.codec.http.HttpHeaders;

import java.util.Map;

import org.springframework.stereotype.Service;

import dl.digger.zeroone.http.CmdHttpContext;
import dl.digger.zeroone.http.ZeroOneHttpResponse;

@Service
public class XmlOut implements Out {

	
	public static final String CONTENT_TYPE = "text/plain;charset=";
	
	public void out(Map<String, Object> result, CmdHttpContext context) {
		ZeroOneHttpResponse response = context.getResponse();
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, CONTENT_TYPE
				+ response.getCharacterEncoding());
		Object data = result.get("data");
		if (data != null) {
			response.write(data.toString());
		}
	}

}
