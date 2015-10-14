package dl.digger.zeroone.http.exception;

public class CmdException extends Exception {

	public final static  CmdException CMD_ERROR = new CmdException(10000, "service unknow error.");
	
	
	final int code;
	final String msg;
	
	public CmdException(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "code=" + code + ",msg=" + msg;
	}

	public int getCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}
}
