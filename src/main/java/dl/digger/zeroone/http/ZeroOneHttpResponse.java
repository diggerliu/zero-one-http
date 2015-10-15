package dl.digger.zeroone.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.util.StringUtils;

import dl.digger.zeroone.http.util.Utils;

public class ZeroOneHttpResponse {

	private final DefaultFullHttpResponse response;
	private final ByteBuf content;
	private String characterEncoding = Utils.DEFAULT_CHARACTERENCODING;

	public ZeroOneHttpResponse() {
		this(Unpooled.buffer(0));
	}

	public ZeroOneHttpResponse(ByteBuf content) {
		this.content = content;
		this.response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.OK, content);
		setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=utf-8");
	}

	public void sendError(int sc) throws IOException {
		this.response.setStatus(HttpResponseStatus.valueOf(sc));
	}

	public void sendError(int sc, String msg) throws IOException {
		this.response.setStatus(new HttpResponseStatus(sc, msg));
	}

	public void sendRedirect(String location) throws IOException {
		setStatus(302);
		setHeader(HttpHeaders.Names.LOCATION, location);
	}

	public void addCookie(Cookie cookie) {
		HttpHeaders.addHeader(this.response, HttpHeaders.Names.SET_COOKIE,
				cookie);
	}

	public void addHeader(String name, String value) {
		HttpHeaders.addHeader(this.response, name, value);
	}

	public void addIntHeader(String name, int value) {
		HttpHeaders.addIntHeader(this.response, name, value);
	}

	public void setStatus(int sc) {
		this.response.setStatus(HttpResponseStatus.valueOf(sc));
	}

	public void setStatus(int sc, String sm) {
		this.response.setStatus(new HttpResponseStatus(sc, sm));
	}

	public void setHeader(String name, String value) {
		HttpHeaders.setHeader(this.response, name, value);
	}

	public DefaultFullHttpResponse getDefaultFullHttpResponse() {
		return response;
	}

	public int getContentSize() {
		return content.readableBytes();
	}

	public void write(String str) {
		if (!StringUtils.isEmpty(str)) {
			content.writeBytes(str.getBytes(Charset.forName(characterEncoding)));
		}
	}

	public void write(byte[] bytes) {
		if (bytes != null && bytes.length > 0) {
			content.writeBytes(bytes);
		}
	}

	public void reset() {
		this.content.release();
	}

	public ByteBuf getContent() {
		return content;
	}

	public String getCharacterEncoding() {
		return characterEncoding;
	}

	public void setCharacterEncoding(String characterEncoding) {
		this.characterEncoding = characterEncoding;
		setHeader(HttpHeaders.Names.CONTENT_ENCODING, characterEncoding);
	}

}
