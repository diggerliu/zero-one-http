package dl.digger.zeroone.http;



public interface HttpCmd {

	public Object process(CmdHttpContext context,ZeroOneHttpRequest request,ZeroOneHttpResponse response) throws Exception;

}
