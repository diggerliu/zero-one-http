package dl.digger.zeroone.http;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Sharable
@Service
public class HttpServerHanlder extends SimpleChannelInboundHandler<HttpRequest> {
	final static Logger logger = LoggerFactory
			.getLogger(HttpServerHanlder.class);

	@Autowired
	private HttpServerConfig config;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg)
			throws Exception {
		ZeroOneHttpRequest request = new ZeroOneHttpRequest(msg);
		config.doBusiness(ctx, request);
		// ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		logger.error("HttpServerHanlder error",cause);
		ctx.writeAndFlush(config.INTERNAL_SERVER_ERROR_RESP);
		ctx.close();
	}
}
