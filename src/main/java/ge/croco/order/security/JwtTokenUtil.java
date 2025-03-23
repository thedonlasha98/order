package ge.croco.order.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {

    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    public static Claims extractAllClaims(String token, String secretKey) {
        return extractAllClaims(token, getSigningKey(secretKey));
    }

    public Claims extractAllClaims(String token) {
        return extractAllClaims(token, getSigningKey(secretKey));
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject(); // Extract the "sub" claim (username)
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class); // Extract the "userId" claim
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token, getSigningKey(secretKey));
        List<String> authorityNames = claims.get("authorities", List.class);

        return authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false; // Token is invalid
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private static Key getSigningKey(String secretKey) {
        return Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secretKey));
    }

    private static Claims extractAllClaims(String token, Key signingKey) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey) // Use the secret key to verify the token
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}