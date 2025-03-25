package ge.croco.order.security;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Objects;

public class JwtClaimArgumentResolver implements HandlerMethodArgumentResolver {

    @Value("${jwt.secret.key}")
    private String secretKey;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JwtClaim.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String authHeader = webRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        String token = authHeader.replace("Bearer ", "");
        Claims claims = JwtTokenUtil.extractAllClaims(token, secretKey);

        JwtClaim annotation = parameter.getParameterAnnotation(JwtClaim.class);
        String claimName = Objects.requireNonNull(annotation).value();
        claimName = claimName.isEmpty() ? parameter.getParameterName() : claimName;

        return claims.get(claimName, parameter.getParameterType());
    }
}