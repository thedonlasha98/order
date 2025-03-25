package ge.croco.order.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule()
                    .addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER))
                    .addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER))
            );

    private static final Set<String> SENSITIVE_FIELDS = Set.of("authorization", "password", "token");

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) && !@annotation(ge.croco.order.annotation.DontLog)")
    private void controller() {
    }

    @Before("controller()")
    protected void beforeLogger(JoinPoint joinPoint) {
        getRequest().ifPresent(request -> {
            String logInfo = new StringBuilder("##### Received Http Request")
                    .append(" ##### Method: ")
                    .append(request.getMethod())
                    .append(" ##### URL: ")
                    .append(request.getRequestURL())
                    .append(" ##### Headers: ")
                    .append(getHeaders(request))
                    .append(" ##### Body: ")
                    .append(getRequestBody(joinPoint))
                    .toString();

            log.info(logInfo);
        });
    }

    @AfterReturning(value = "controller()", returning = "retVal")
    protected void afterLogger(Object retVal) {
        getResponse().ifPresent(response -> {
            StringBuilder logInfo = new StringBuilder("##### Dispatched Http Response:")
                    .append(" ##### Status Code: ")
                    .append(response.getStatus());

            if (retVal != null) {
                logInfo.append(" ##### Response Body: ")
                        .append(maskSensitiveFields(tryConvertToJson(retVal)));
            }

            log.info(logInfo.toString());
        });
    }

    private Optional<HttpServletRequest> getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attributes)
                .map(ServletRequestAttributes::getRequest);
    }

    private Optional<HttpServletResponse> getResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attributes)
                .map(ServletRequestAttributes::getResponse);
    }

    private String getRequestBody(JoinPoint joinPoint) {
        try {
            Method methodSignature = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Parameter[] parameters = methodSignature.getParameters();
            Object requestBody = null;
            for (int i = 0; i < parameters.length; i++) {
                var parameter = parameters[i];
                var requestBodyAnnotation = parameter.getAnnotation(RequestBody.class);
                if (requestBodyAnnotation != null) {
                    requestBody = joinPoint.getArgs()[i];
                }
            }
            return maskSensitiveFields(OBJECT_MAPPER.writeValueAsString(requestBody));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headersMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (SENSITIVE_FIELDS.contains(headerName.toLowerCase())) {
                headerValue = "*****";
            }
            headersMap.put(headerName, headerValue);
        }

        return headersMap;
    }

    public static String maskSensitiveFields(String json) {
        if (json == null) return null;

        for (String field : SENSITIVE_FIELDS) {
            String regex = "(?i)(\"" + Pattern.quote(field) + "\"\\s*:\\s*\")[^\"]*(\")";
            json = json.replaceAll(regex, "$1*****$2");
        }

        return json;
    }

    private static String tryConvertToJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

}
