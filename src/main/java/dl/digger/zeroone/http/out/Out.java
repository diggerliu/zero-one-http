package dl.digger.zeroone.http.out;

import java.util.Map;

import dl.digger.zeroone.http.CmdHttpContext;

public interface Out {

	public void out(Map<String, Object> obj, CmdHttpContext context);

}
