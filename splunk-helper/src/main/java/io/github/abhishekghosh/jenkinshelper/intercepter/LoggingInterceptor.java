package io.github.abhishekghosh.jenkinshelper.intercepter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Value("${spring.application.name}")
    String appName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> context = new HashMap<>();
        String uid = UUID.randomUUID().toString();
        context.put("uid", uid);
        context.put("uri", request.getRequestURI());
        logger.info("Request URI: {}", request.getRequestURI());
        context.put("method", request.getMethod());
        context.put("startTime", String.valueOf(System.currentTimeMillis()));
        context.put("appName", appName);
        ThreadContext.putAll(context);

        logger.info("Intercepted Request CF_UID: {}", uid);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        Map<String, String> context = MDC.getCopyOfContextMap();
        if (!CollectionUtils.isEmpty(context)) {
            long responseTime = System.currentTimeMillis() - Long.parseLong(context.get("startTime"));
            String uuid = context.get("uid");
            logger.debug("transaction completed for uuid {}", uuid);
            logger.info("ResponseCode={}|ResponseTime={}ms", response.getStatus(), responseTime);
            ThreadContext.clearMap();
            MDC.clear();
        }

    }
}
