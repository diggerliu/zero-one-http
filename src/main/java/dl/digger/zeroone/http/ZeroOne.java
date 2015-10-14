package dl.digger.zeroone.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ZeroOne {
	final static Logger logger = LoggerFactory.getLogger(ZeroOne.class);

	private ApplicationContext context;
	private HttpServerConfig config;

	public ZeroOne(final Class<?>... classes) {
		this(new DefaultHttpServerInitializer(),classes);
	}
	
	public ZeroOne(HttpServerInitializer initializer, final Class<?>... classes) {
		if (classes == null || classes.length == 0) {
			this.context = new AnnotationConfigApplicationContext(
					HttpServerConfig.class);
		} else {
			Class<?>[] classes_new = new Class<?>[classes.length + 1];
			classes_new[0] = HttpServerConfig.class;
			int index = 1;
			for (Class<?> c : classes) {
				classes_new[index++] = c;
			}
			this.context = new AnnotationConfigApplicationContext(classes_new);
		}
		this.config = context.getBean(HttpServerConfig.class);
		this.config.init(context,initializer);
	}
	

	public void start() {
		ServerBootstrap bootstrap = new ServerBootstrap();
		InetSocketAddress bindAddr = config.getAddr();
		ChannelFuture f = bootstrap.channel(NioServerSocketChannel.class)
				.group(new NioEventLoopGroup(2), new NioEventLoopGroup())
				.childHandler(config.getHttpServerChannelInitializer())
				.bind(bindAddr);
		try {
			ChannelFuture future = f.sync();
			if (future.isSuccess()) {
				logger.error(
						"God bless you!Server started successfully!On server={},port={}",
						bindAddr.getHostName(), bindAddr.getPort());
			}
		} catch (InterruptedException e) {
			logger.error("start interruptedException,system will exit!!", e);
			System.exit(1);
		} catch (Exception e) {
			logger.error("start exception,system will exit!!", e);
			System.exit(1);
		} finally {
			logger.error("\n#@##@##@##@##@##@##@##@#~_~Zero-One-HTTP-Server on port {}~_~#@##@##@##@##@##@##@##@#\n", bindAddr.getPort());
		}
	}

	public static void main(String[] args) {
		ZeroOne app = new ZeroOne();
		app.start();
	}
}
