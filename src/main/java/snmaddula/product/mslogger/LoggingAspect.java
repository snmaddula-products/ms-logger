package snmaddula.product.mslogger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class LoggingAspect {

	@Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
	private void inRestControllers() {
	}

	@Pointcut("@within(org.springframework.stereotype.Service)")
	private void inServices() {
	}

	@Pointcut("@within(org.springframework.stereotype.Component)")
	private void inComponents() {
	}

	@Pointcut("@within(org.springframework.stereotype.Configuration)")
	private void inConfigurations() {
	}

	@Pointcut("inRestControllers() || inServices() || inComponents() || inConfigurations()")
	public void aopAwareClasses() {
	}

	@Around("aopAwareClasses()")
	public Object logMethodExecution(ProceedingJoinPoint jp) throws Throwable {
		logWhenEnteringMethod(jp);

		final StopWatch watch = new StopWatch();
		watch.start();

		final Object retval = jp.proceed();
		watch.stop();

		logWhenLeavingMethod(jp, retval, watch);
		return retval;
	}

	@AfterThrowing(pointcut = "aopAwareClasses()", throwing = "ex")
	public void logWhenExceptionWasThrown(JoinPoint jp, Exception ex) {
		final Signature signature = jp.getSignature();

		final StringBuilder sb = new StringBuilder();
		sb.append("Failed ").append(signature.getName()).append(" [");
		appendParamNamesWithArgs(sb, jp);
		sb.append("] thrown [").append(ex.getClass().getName()).append(" with message ").append(ex.getMessage());

		logException(jp, sb, ex);
	}

	private void logWhenEnteringMethod(JoinPoint jp) {
		final Signature signature = jp.getSignature();

		final StringBuilder sb = new StringBuilder();
		sb.append("Started ").append(signature.getName()).append(" [");
		appendParamNamesWithArgs(sb, jp);
		sb.append("]");

		log(jp, sb);
	}

	private void logWhenLeavingMethod(final JoinPoint jp, final Object retval, final StopWatch watch) {
		final Signature signature = jp.getSignature();

		final StringBuilder sb = new StringBuilder();
		sb.append("Finished ").append(signature.getName()).append(" [");
		appendParamNamesWithArgs(sb, jp);
		sb.append("] returned [").append(retval).append("] in ").append(watch.getTotalTimeMillis()).append(" ms");

		log(jp, sb);
	}

	private void log(final JoinPoint jp, final StringBuilder sb) {
		final Logger logger = LoggerFactory.getLogger(jp.getTarget().getClass().getName());
		logger.info(sb.toString());
	}

	private void appendParamNamesWithArgs(final StringBuilder sb, final JoinPoint jp) {
		final String[] parameterNames = getParameterNames(jp);
		final Object[] args = jp.getArgs();
		for (int i = 0; i < args.length; i++) {
			if (parameterNames != null) {
				sb.append(parameterNames[i]).append("=").append(args[i]).append(",");
			} else {
				sb.append(args[i]).append(",");
			}
		}

		if (args.length > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}

	}

	private String[] getParameterNames(final JoinPoint jp) {
		final MethodSignature signature = (MethodSignature) jp.getSignature();
		return (signature != null) ? signature.getParameterNames() : null;
	}

	private void logException(final JoinPoint jp, final StringBuilder sb, final Exception ex) {
		final Logger logger = LoggerFactory.getLogger(jp.getTarget().getClass().getName());
		final String logMessage = sb.toString();
		if (nonCriticalException(ex)) {
			logger.warn(logMessage, ex);
		} else {
			logger.error(logMessage, ex);
		}
	}

	private boolean nonCriticalException(final Exception ex) {
		//TODO:: nonCriticalExceptions.contains(ex) ? true : false
		return false;
	}
}
