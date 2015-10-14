package dl.digger.zeroone.http.out;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dl.digger.zeroone.http.CmdHttpContext;
import dl.digger.zeroone.http.ZeroOneHttpResponse;

@Service
public class JsonOut implements Out {

	private final static ObjectMapper mapper = new ObjectMapper();
	final Logger logger = LoggerFactory.getLogger(getClass());

	public static final byte[] DEFAULT_OUT_BYTES = "{\"rtn\":0}".getBytes();
	public static final byte[] ERROR_DEFAULT_OUT_BYTES = "{\"rtn\":1000}"
			.getBytes();

	public void out(Map<String, Object> result, CmdHttpContext context) {
		ZeroOneHttpResponse response = context.getResponse();
		try {
			String str = mapper.writeValueAsString(result);
			response.write(str);
		} catch (JsonProcessingException e) {
			logger.error("Json Out JsonProcessingException", e);
			response.write(ERROR_DEFAULT_OUT_BYTES);
		}
	}

}
