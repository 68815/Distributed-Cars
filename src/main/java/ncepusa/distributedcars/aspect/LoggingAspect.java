package ncepusa.distributedcars.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * <p>Spring AOP日志切面类</p>
 *
 * @author 0109
 * @since 2025-09-03
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * 切入点：所有在ncepusa.distributedcars包及其子包中的public方法
     */
    @Pointcut("execution(* ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener.readDataFromRedis(String)) || " +
            "execution(* ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener.generatePath(String)) || " +
            "execution(* ncepusa.distributedcars.navigator.message_queue_interaction.ActiveMQListener.writePathToRedis(String))")
    public void applicationPackagePointcut() {
    }

    /**
     * 环绕通知：记录方法执行日志
     */
    @Around("applicationPackagePointcut()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // 记录方法开始执行
        logger.info("开始执行方法: {}::{}(参数: {})", className, methodName, joinPoint.getArgs());

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            stopWatch.stop();

            // 记录方法成功执行完成
            logger.info("方法执行完成: {}::{} - 执行时间: {} ms", className, methodName, stopWatch.getTotalTimeMillis());
            
            return result;
        } catch (Exception e) {
            stopWatch.stop();
            
            // 记录方法执行异常
            logger.error("方法执行异常: {}::{} - 异常信息: {} - 执行时间: {} ms", 
                className, methodName, e.getMessage(), stopWatch.getTotalTimeMillis());
            
            throw e;
        }
    }
}