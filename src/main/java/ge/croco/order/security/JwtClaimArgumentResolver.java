package ge.croco.order.security;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
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
        // Check if the parameter is annotated with @JwtClaim
        return parameter.hasParameterAnnotation(JwtClaim.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // Get the Authorization header from the request
        String authHeader = webRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }

        // Extract the JWT token
        String token = authHeader.replace("Bearer ", "");
        // Parse the JWT and extract the claims
        Claims claims = JwtTokenUtil.extractAllClaims(token, secretKey);

        // Get the claim name from the annotation
        JwtClaim annotation = parameter.getParameterAnnotation(JwtClaim.class);
        String claimName = Objects.requireNonNull(annotation).value();

        // Extract and return the claim value
        return claims.get(claimName);
    }
}