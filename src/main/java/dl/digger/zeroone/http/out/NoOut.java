package dl.digger.zeroone.http.out;

import java.util.Map;

import org.springframework.stereotype.Service;

import dl.digger.zeroone.http.CmdHttpContext;
@Service
public class NoOut implements Out {

	public void out(Map<String, Object> obj, CmdHttpContext context) {
		return;
	}

}
