package dl.digger.zeroone.http.out;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dl.digger.zeroone.http.CmdHttpContext;
import dl.digger.zeroone.http.ZeroOneHttpResponse;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

@Service
public class HtmlOut implements Out {

	private Configuration config;
	final Logger logger = LoggerFactory.getLogger(getClass());
	@Value("${zero.one.server.http.freemarker.template:/data/template}")
	private String template;

	@Autowired
	private void init() throws IOException {
		config = new Configuration(Configuration.VERSION_2_3_23);
		config.setDirectoryForTemplateLoading(new File(template));
		config.setObjectWrapper(new DefaultObjectWrapper(
				Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS));
	}

	public void out(Map<String, Object> result, CmdHttpContext context) {
		ZeroOneHttpResponse response = context.getResponse();
		try {
			Template template = config.getTemplate(context.getAdapter()
					.getTemplate(), "UTF-8");
			StringWriter out = new StringWriter();
			template.process(result, out);
			response.write(out.toString());
		} catch (Exception e) {
			logger.error("HtmlOut error", e);
			response.write("html out error".getBytes());
		}
		// template.process(obj, out);
	}

}
