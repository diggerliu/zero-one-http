package dl.digger.zeroone.http.out;

import java.util.Map;

import org.springframework.stereotype.Service;

import dl.digger.zeroone.http.CmdHttpContext;

@Service
public class TextOut implements Out{

	public void out(Map<String, Object> result,CmdHttpContext context){
		result.get("rtn");
	}



}
