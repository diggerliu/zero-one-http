package dl.digger.zeroone.http;

import io.netty.channel.ChannelHandlerContext;

public class CmdHttpContext {
	private ZeroOneHttpRequest request;
	private ZeroOneHttpResponse response;
	private long startTime;
	private CmdHttpAdapter adapter;
	private ChannelHandlerContext ctx;

	public CmdHttpContext(ZeroOneHttpRequest request,
			ZeroOneHttpResponse response, CmdHttpAdapter adapter,ChannelHandlerContext ctx) {
		super();
		this.request = request;
		this.response = response;
		this.startTime = System.currentTimeMillis();
		this.adapter = adapter;
		this.ctx = ctx;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(System.currentTimeMillis()-startTime).append(",").append(request.toString());
		
		return sb.toString();
	}

	public ZeroOneHttpResponse getResponse() {
		return response;
	}

	public void setResponse(ZeroOneHttpResponse response) {
		this.response = response;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}



	public ZeroOneHttpRequest getRequest() {
		return request;
	}



	public void setRequest(ZeroOneHttpRequest request) {
		this.request = request;
	}



	public CmdHttpAdapter getAdapter() {
		return adapter;
	}



	public void setAdapter(CmdHttpAdapter adapter) {
		this.adapter = adapter;
	}



	public ChannelHandlerContext getCtx() {
		return ctx;
	}



	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}
}
