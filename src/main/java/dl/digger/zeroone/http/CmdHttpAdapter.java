package dl.digger.zeroone.http;

import java.util.Map;

import dl.digger.zeroone.http.out.Out;


public class CmdHttpAdapter {

	private String id;
	private HttpCmd cmd;
	private int timeout = 1000;// default 1s
	private Out out;
	private String referer;
	private boolean csrf;
	private boolean postOnly;
	private String template;

	public CmdHttpAdapter(String id, HttpCmd cmd,Out out) {
		super();
		this.id = id;
		this.cmd = cmd;
		this.out = out;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public HttpCmd getCmd() {
		return cmd;
	}

	public void setCmd(HttpCmd cmd) {
		this.cmd = cmd;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public Object process(CmdHttpContext context) throws Exception{
		return cmd.process(context,context.getRequest(),context.getResponse());
	}
	public void out(Map<String,Object> result,CmdHttpContext context){
		out.out(result,context);
	}

	public Out getOut() {
		return out;
	}

	public void setOut(Out out) {
		this.out = out;
	}

	public String getReferer() {
		return referer;
	}

	public void setReferer(String referer) {
		this.referer = referer;
	}

	public boolean isCsrf() {
		return csrf;
	}

	public void setCsrf(boolean csrf) {
		this.csrf = csrf;
	}

	public boolean isPostOnly() {
		return postOnly;
	}

	public void setPostOnly(boolean postOnly) {
		this.postOnly = postOnly;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}
}
