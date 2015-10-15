package dl.digger.zeroone.http.out;

import io.netty.handler.codec.http.HttpHeaders;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import dl.digger.zeroone.http.CmdHttpContext;
import dl.digger.zeroone.http.ZeroOneHttpResponse;

@Service
public class XmlOut implements Out {

	public static final String CONTENT_TYPE = "text/xml;charset=";
	private final XmlMapper xmlMapper = new XmlMapper();
	final Logger logger = LoggerFactory.getLogger(getClass());
	public static final byte[] ERROR_DEFAULT_OUT_BYTES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result><rtn>1000</rtn><msg>XmlOut error</msg></result>"
			.getBytes();

	public void out(Map<String, Object> result, CmdHttpContext context) {

		ZeroOneHttpResponse response = context.getResponse();
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, CONTENT_TYPE
				+ response.getCharacterEncoding());
		try {
			String result_str = toXML(result,"result",response.getCharacterEncoding());
			response.write(result_str);
		} catch (IOException e) {
			logger.error("XmlOut error", e);
			response.write(ERROR_DEFAULT_OUT_BYTES);
		}
	}

	public String toXML(Object object, String rootName ,String characterEncoding) throws IOException {
		// Object to XML
		String xmlStr = xmlMapper.writeValueAsString(object);
		String xmlHeader = "<?xml version=\"1.0\" encoding=\""+characterEncoding+"\"?>";
		// Object Class Name
		String mapClassName = object.getClass().getSimpleName();
		String beginStr = "<" + mapClassName + ">";
		String endStr = "</" + mapClassName + ">";
		int beginNum = beginStr.length();
		int endNum = xmlStr.indexOf(endStr);
		String subStr = xmlStr.substring(beginNum, endNum);
		StringBuffer sb = new StringBuffer();
		sb.append(xmlHeader).append("<" + rootName + ">").append(subStr).append("</" + rootName + ">");  
		return sb.toString();
	}
}
