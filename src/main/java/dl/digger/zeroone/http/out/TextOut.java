package dl.digger.zeroone.http.out;

import java.util.Map;

import org.springframework.stereotype.Service;

import dl.digger.zeroone.http.CmdHttpContext;
import dl.digger.zeroone.http.ZeroOneHttpResponse;

@Service
public class TextOut implements Out {

	public void out(Map<String, Object> result, CmdHttpContext context) {
		ZeroOneHttpResponse response = context.getResponse();
		Object data = result.get("data");
		if (data != null) {
			response.write(data.toString());
		}
	}

}
