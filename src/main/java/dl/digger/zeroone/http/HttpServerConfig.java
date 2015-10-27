package dl.digger.zeroone.http;

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

import java.io.File;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import dl.digger.zeroone.http.exception.CmdException;
import dl.digger.zeroone.http.out.JsonOut;
import dl.digger.zeroone.http.out.NoOut;
import dl.digger.zeroone.http.out.Out;
import dl.digger.zeroone.http.out.annotation.HttpConfig;
import dl.digger.zeroone.http.util.ErrorCode;
import dl.digger.zeroone.http.util.Utils;

@ComponentScan
@Configuration
@PropertySources(value = { @PropertySource(value = "classpath:zero-one-http.properties", name = "zero-one") })
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

	@Autowired
	private Environment env;

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
					String referers = httpconfig.referer();
					if (!StringUtils.isEmpty(referers)) {
						String[] referer_array = referers.split("\\|");
						adapter.setReferers(referer_array);
					}

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
		}
		logger.error(
				"\n#####################################\nuriMaps:{}\n#####################################\n",
				uriMaps.keySet());
		liveCmds = new LinkedBlockingQueue<CmdHttpContext>(
				this.max_live_context);
		initializer.init();
		this.initializer = initializer;
		ExecutorService service = Executors.newFixedThreadPool(work_thread_num);
		for (int i = 0; i < work_thread_num; i++) {
			service.execute(new Task());
		}
	}

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
					int code = ErrorCode.OK;
					Object result_obj = null;
					String msg = null;
					try {
						result_obj = adapter.process(context);
					} catch (CmdException e) {
						// TODO
						logger.error("service error:", e);
						code = e.getCode();
						msg = e.getMsg();
					} catch (Exception e) {
						logger.error("unkown error", e);
						code = ErrorCode.CMD_SERVICE_UNKNOW_ERROR;
						msg = e.getMessage();
					} finally {
						out(code, result_obj, msg, context);
					}

				}
			}
		}
	}

	private void out(int rtn, Object data, String msg, CmdHttpContext context) {
		CmdHttpAdapter adapter = context.getAdapter();
		Map<String, Object> result = null;
		if (!(adapter.getOut() instanceof NoOut)) {
			result = Utils.getResultMap(rtn, data, msg);
			adapter.out(result, context);
		}
		DefaultFullHttpResponse respnose = context.getResponse()
				.getDefaultFullHttpResponse();
		ACCESS_LOGGER.info("200,{},{},{},{}", rtn, System.currentTimeMillis()
				- context.getStartTime(), context.getRequest().getAllParams(),
				msg);

		ChannelFuture future = context.getCtx().writeAndFlush(respnose);
		boolean keepAlive = HttpHeaders.isKeepAlive(context.getRequest()
				.getRequest());
		if (keepAlive) {
			HttpHeaders headers = respnose.headers();
			headers.set(HttpHeaders.Names.CONTENT_LENGTH,
					String.valueOf(respnose.content().readableBytes()));
			headers.set(HttpHeaders.Names.CONNECTION,
					HttpHeaders.Values.KEEP_ALIVE);
		} else {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	public void doBusiness(ChannelHandlerContext ctx, ZeroOneHttpRequest request) {
		String id = request.getReqId();
		if (uriMaps == null) {
			// TODO logger
			ACCESS_LOGGER.error("501,id={} is not found!!", id);
			ctx.writeAndFlush(Utils.NOT_IMPLEMENTED_RESP);
			ChannelFuture future = ctx.writeAndFlush(Utils.NOT_FOUND_RESP);
			if (future.isDone()) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
			return;
		}

		CmdHttpAdapter adapter = uriMaps.get(id);
		if (adapter == null) {
			// TODO logger
			ACCESS_LOGGER.info("404,id={} is not found!!", id);
			ChannelFuture future = ctx.writeAndFlush(Utils.NOT_FOUND_RESP);
			if (future.isDone()) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
			return;
		}
		// TODO directBuffer ?
		ZeroOneHttpResponse response = new ZeroOneHttpResponse(
				Unpooled.buffer(0));// default 512 bytes

		CmdHttpContext context = new CmdHttpContext(request, response, adapter,
				ctx);
		int check_code = checkRequest(adapter, request);
		if (check_code == 0) {
			try {
				liveCmds.offer(context, 500, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				logger.error(
						"liveCmds offer error,InterruptedException close ctx",
						e);
				ctx.close();
			} catch (NullPointerException e) {
				logger.error(
						"liveCmds offer error ,NullPointerException close ctx",
						e);
				ctx.close();
			} catch (Throwable e) {
				logger.error("liveCmds offer error ,Throwable close ctx", e);
				ctx.close();
			}
		} else {
			out(check_code, null, "", context);

		}

	}

	private int checkRequest(CmdHttpAdapter adapter, ZeroOneHttpRequest request) {
		// referer check
		String[] referers = adapter.getReferers();
		if (!StringUtils.isEmpty(referers)) {
			boolean isReferer = false;
			String req_referer = request.getReferer();
			if (StringUtils.isEmpty(req_referer)) {
				for (String referer : referers) {
					if (referer.equals("null")) {
						isReferer = true;
						break;
					}
				}
			}else{
				try {
					URL url = new URL(req_referer);
					String host = url.getHost();
					//String scheme = url.getProtocol();
					for (String referer : referers) {
						if (StringUtils.endsWithIgnoreCase(host, referer)) {
							isReferer = true;
							break;
						}
					}
				} catch (MalformedURLException e) {
					logger.error("url excetion", e);
					return ErrorCode.CMD_REFERER_ERROR;
				}
			}
			if (!isReferer) {
				logger.error("referer error {}", request.getReferer());
				return ErrorCode.CMD_REFERER_ERROR;
			}
		}
		// postOnly check
		if(adapter.isPostOnly()){
			if(!request.getMethod().equals("POST")){
				return ErrorCode.CMD_POST_ONLY;
			}
		}
		// param check?csrf check?
		// TODO
		return 0;
	}

	public ApplicationContext getContext() {
		return context;
	}

	public InetSocketAddress getAddr() {
		if (this.ip != null) {
			return new InetSocketAddress(this.ip, this.port);
		}
		if (this.nic != null) {
			String ip = Utils.resolveNicAddr(this.nic);
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

	public Environment getEnv() {
		return env;
	}

}
