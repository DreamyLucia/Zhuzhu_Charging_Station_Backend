package org.zhuzhu_charging_station_backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.zhuzhu_charging_station_backend.entity.User;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 生成 Token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // 添加roles声明
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        // 如果UserDetails是我们的User实体，使用用户ID作为subject
        String subject = userDetails instanceof User ?
                ((User) userDetails).getUserId().toString() :
                userDetails.getUsername();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // 使用用户ID作为sub
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 验证 Token
    public boolean validateToken(String token, UserDetails userDetails) {
        final String subject = extractClaim(token, Claims::getSubject);
        return subject.equals(((User) userDetails).getUserId().toString())
                && !isTokenExpired(token);
    }

    // 从Token中提取用户ID
    public Long extractUserId(String token) {
        return Long.parseLong(extractClaim(token, Claims::getSubject));
    }

    // 从Token中提取角色列表
    public List<String> extractRoles(String token) {
        return extractAllClaims(token).get("roles", List.class);
    }

    // 检查 Token 是否过期
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 提取 Token 过期时间
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        // 使用 secret 字段生成密钥
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}