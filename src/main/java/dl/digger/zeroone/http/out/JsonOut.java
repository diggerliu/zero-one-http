package dl.digger.zeroone.http.out;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dl.digger.zeroone.http.CmdHttpContext;
import dl.digger.zeroone.http.ZeroOneHttpRequest;

@Service
public class JsonOut implements Out {

	private final static ObjectMapper mapper = new ObjectMapper();
	final Logger logger = LoggerFactory.getLogger(getClass());

	public static final byte[] DEFAULT_OUT_BYTES = "{\"rtn\":0}".getBytes();
	public static final byte[] ERROR_DEFAULT_OUT_BYTES = "{\"rtn\":1000}"
			.getBytes();

	public void out(Map<String, Object> result, CmdHttpContext context) {
		ZeroOneHttpRequest request = context.getRequest();
		DefaultFullHttpResponse response = context.getResponse();
		ByteBuf content = response.content();
		try {
			byte[] bytes = mapper.writeValueAsString(result).getBytes(request.getCharacterEncoding());
			content.writeBytes(bytes);
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException ",e);
			content.writeBytes(ERROR_DEFAULT_OUT_BYTES);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			logger.error("Json Out JsonProcessingException",e);
			content.writeBytes(ERROR_DEFAULT_OUT_BYTES);
		}

	}


}
