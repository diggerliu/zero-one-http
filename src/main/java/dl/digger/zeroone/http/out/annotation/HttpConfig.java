package dl.digger.zeroone.http.out.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dl.digger.zeroone.http.out.JsonOut;
import dl.digger.zeroone.http.out.Out;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HttpConfig {

	String value() default "";

	String name() default "";

	boolean postOnly() default false;

	String referer() default "NULL|*";

	boolean csrf() default false;

	Class<? extends Out> out() default JsonOut.class;
	
	String template() default "";
}
