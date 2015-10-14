package dl.digger.zeroone.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.util.StringUtils;

import dl.digger.zeroone.http.exception.CmdException;
import dl.digger.zeroone.http.out.HtmlOut;
import dl.digger.zeroone.http.out.JsonOut;
import dl.digger.zeroone.http.out.Out;
import dl.digger.zeroone.http.out.annotation.HttpConfig;
import dl.digger.zeroone.http.util.ZeroOneErrorCode;
import dl.digger.zeroone.http.util.ZeroOneUtil;

@ComponentScan
@Configuration
@PropertySource(value = "classpath:zero-one-http.properties", name = "zero-one")
public class HttpServerConfig {

	final Logger logger = LoggerFactory.getLogger(HttpServerConfig.class);
	final Logger ACCESS_LOGGER = LoggerFactory.getLogger("ACCESS");
	@Value("${zero.one.server.http.port:8088}")
	private int port;
	@Value("${zero.one.server.http.nic:null}")
	private String nic;
	@Value("${zero.one.server.http.ip:null}")
	private String ip;
	@Value("${zero.one.server.http.max_live_context:2000}")
	private int max_live_context;
	@Value("${zero.one.server.http.work_thread_num:8}")
	private int work_thread_num;
	@Autowired
	private HttpServerHanlder httpServerHanlder;

	private HttpServerInitializer initializer;
	private Map<String, CmdHttpAdapter> uriMaps;
	private ApplicationContext context;
	private LinkedBlockingQueue<CmdHttpContext> liveCmds;

	public HttpServerInitializer getInitializer() {
		return initializer;
	}

	public void init(ApplicationContext context,
			HttpServerInitializer initializer) {
		this.context = context;
		logger.debug("HttpServerHanlder init!");
		uriMaps = new HashMap<String, CmdHttpAdapter>();
		Map<String, HttpCmd> maps = context.getBeansOfType(HttpCmd.class);
		Out defaultout = context.getBean(JsonOut.class);
		if (maps == null || maps.size() == 0) {
			logger.error("maps is empty");
		} else {
			logger.error("maps {}", maps);
			for (Map.Entry<String, HttpCmd> cmd : maps.entrySet()) {
				HttpCmd httpcmd = cmd.getValue();
				HttpConfig httpconfig = httpcmd.getClass().getAnnotation(
						HttpConfig.class);
				String id = "/" + cmd.getKey();
				CmdHttpAdapter adapter = new CmdHttpAdapter(id, httpcmd,
						defaultout);
				if (httpconfig != null) {
					if (!StringUtils.isEmpty(httpconfig.value())) {
						id = httpconfig.value();
						adapter.setId(id);
					}
					adapter.setReferer(httpconfig.referer());
					adapter.setCsrf(httpconfig.csrf());
					adapter.setPostOnly(httpconfig.postOnly());
					Class<? extends Out> outclazz = httpconfig.out();
					Out out = context.getBean(outclazz);
					adapter.setOut(out);
					if (StringUtils.isEmpty(httpconfig.template())) {
						adapter.setTemplate(File.pathSeparator
								+ (httpconfig.value() == null ? cmd.getKey()
										: httpconfig.value()));
					} else {
						String template = httpconfig.template();
						adapter.setTemplate(template);
					}
				}
				uriMaps.put(id, adapter);
			}
			logger.error(
					"\n#####################################\nuriMaps:{}\n#####################################\n",
					uriMaps.keySet());
		}
		liveCmds = new LinkedBlockingQueue<CmdHttpContext>(
				this.max_live_context);
		initializer.init();
		this.initializer = initializer;
		ExecutorService service = Executors.newFixedThreadPool(work_thread_num);
		for (int i = 0; i < work_thread_num; i++) {
			service.execute(new Task());
		}
	}

	public final DefaultFullHttpResponse NOT_FOUND_RESP = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
	public final DefaultFullHttpResponse INTERNAL_SERVER_ERROR_RESP = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
	public final DefaultFullHttpResponse NOT_IMPLEMENTED_RESP = new DefaultFullHttpResponse(
			HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_IMPLEMENTED);

	private final byte[] EMPTY_BYTE = new byte[0];

	public class Task implements Runnable {

		public void run() {
			while (true) {
				CmdHttpContext context = null;
				try {
					context = liveCmds.take();
				} catch (InterruptedException e) {
					logger.error("take error:", e);
				}
				if (context != null) {
					CmdHttpAdapter adapter = context.getAdapter();
					int code = ZeroOneErrorCode.OK;
					Object result_obj = null;
					String msg = null;
					Map<String, Object> result = null;
					try {
						Object obj = adapter.process(context);
						result_obj = obj;
					} catch (CmdException e) {
						// TODO 这两个异常是否有必要分开？ 日志、监控？
						logger.error("service error:", e);
						code = e.getCode();
						msg = e.getMsg();
					} catch (Exception e) {
						logger.error("unkown error", e);
						code = ZeroOneErrorCode.CMD_SERVICE_UNKNOW_ERROR;
						msg = e.getMessage();
					} finally {
						result = ZeroOneUtil
								.getResultMap(code, result_obj, msg);
						adapter.out(result, context);
						// TODO 流水日志
						DefaultFullHttpResponse respnose = context
								.getResponse();
						ChannelFuture future = context.getCtx().writeAndFlush(
								respnose);
						ACCESS_LOGGER.info("200,{},{},{}", code, context, msg);
						boolean keepAlive = HttpHeaders.isKeepAlive(context
								.getRequest().getRequest());
						if (keepAlive) {
							HttpHeaders headers = respnose.headers();
							headers.set(HttpHeaders.Names.CONTENT_LENGTH,
									String.valueOf(respnose.content()
											.readableBytes()));
							headers.set(HttpHeaders.Names.CONNECTION,
									HttpHeaders.Values.KEEP_ALIVE);
						} else {
							future.addListener(ChannelFutureListener.CLOSE);
						}
					}

				}
			}
		}
	}

	public void doBusiness(ChannelHandlerContext ctx, ZeroOneHttpRequest request) {
		String id = request.getReqId();
		if (uriMaps == null) {
			// TODO logger
			ACCESS_LOGGER.error("501,id={} is not found!!", id);
			ctx.writeAndFlush(NOT_IMPLEMENTED_RESP);
			ChannelFuture future = ctx.writeAndFlush(NOT_FOUND_RESP);
			if (future.isDone()) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
			return;
		}

		CmdHttpAdapter adapter = uriMaps.get(id);
		if (adapter == null) {
			// TODO logger
			ACCESS_LOGGER.info("404,id={} is not found!!", id);
			ChannelFuture future = ctx.writeAndFlush(NOT_FOUND_RESP);
			if (future.isDone()) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
			return;
		}
		checkRequest(adapter, request);
		// TODO 是否要切到directBuffer
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
				Unpooled.buffer(512));// 默认给512个字节
		// HttpHeaders headers = response.headers();
		// headers.add();

		CmdHttpContext context = new CmdHttpContext(request, response, adapter,
				ctx);
		try {
			liveCmds.offer(context, 500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.error("liveCmds offer error,InterruptedException close ctx",
					e);
			ctx.close();
		} catch (NullPointerException e) {
			logger.error(
					"liveCmds offer error ,NullPointerException close ctx", e);
			ctx.close();
		} catch (Throwable e) {
			logger.error("liveCmds offer error ,Throwable close ctx", e);
			ctx.close();
		}

	}

	private void checkRequest(CmdHttpAdapter adapter, ZeroOneHttpRequest request) {
		// referer check

		// csrf check

		// postOnly check

		// param check?
	}

	public ApplicationContext getContext() {
		return context;
	}

	public InetSocketAddress getAddr() {
		if (this.ip != null) {
			return new InetSocketAddress(this.ip, this.port);
		}
		if (this.nic != null) {
			String ip = ZeroOneUtil.resolveNicAddr(this.nic);
			return new InetSocketAddress(ip, this.port);
		}
		return new InetSocketAddress(this.port);
	}

	public int getPort() {
		return port;
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		PropertySourcesPlaceholderConfigurer config = new PropertySourcesPlaceholderConfigurer();
		config.setNullValue("null");
		// config.setIgnoreUnresolvablePlaceholders(true);
		return config;
	}

	public HttpServerChannelInitializer getHttpServerChannelInitializer() {
		HttpServerChannelInitializer initalizer = new HttpServerChannelInitializer(
				httpServerHanlder);
		return initalizer;
	}

	public static class HttpServerChannelInitializer extends
			ChannelInitializer<SocketChannel> {
		private HttpServerHanlder httpServerHanlder;

		HttpServerChannelInitializer(HttpServerHanlder httpServerHanlder) {
			this.httpServerHanlder = httpServerHanlder;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ChannelPipeline pipeline = ch.pipeline();
			pipeline.addLast(new HttpRequestDecoder());
			pipeline.addLast(new HttpResponseEncoder());
			pipeline.addLast(new HttpObjectAggregator(1024 * 1024 * 32));
			pipeline.addLast(new HttpContentCompressor());
			pipeline.addLast(httpServerHanlder);
		}

	}

	public int getMax_live_context() {
		return max_live_context;
	}

}
