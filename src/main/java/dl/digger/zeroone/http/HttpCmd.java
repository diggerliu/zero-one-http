package dl.digger.zeroone.http;

import io.netty.handler.codec.http.DefaultFullHttpResponse;


public interface HttpCmd {

	public Object process(CmdHttpContext context,ZeroOneHttpRequest request,DefaultFullHttpResponse response) throws Exception;

}
