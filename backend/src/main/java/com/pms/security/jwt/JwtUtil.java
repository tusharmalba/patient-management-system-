package com.pms.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                       JWT UTILITY CLASS                              ║
 * ║                                                                       ║
 * ║  ┌─────────────────────────────────────────────────────────────┐     ║
 * ║  │                  JWT STRUCTURE                               │     ║
 * ║  │                                                              │     ║
 * ║  │  eyJhbGc.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSJ9.SflKxwRJSMeKKF  │     ║
 * ║  │  ─────────  ─────────────────────────────  ──────────────── │     ║
 * ║  │   HEADER           PAYLOAD                   SIGNATURE      │     ║
 * ║  │  (Base64)          (Base64)                (HMAC-SHA256)    │     ║
 * ║  └──────────────────────────────────────────────────────────── ┘     ║
 * ║                                                                       ║
 * ║  HEADER: { "alg": "HS256", "typ": "JWT" }                            ║
 * ║                                                                       ║
 * ║  PAYLOAD (Claims):                                                    ║
 * ║    "sub": "user@email.com"  (subject = who this token is for)        ║
 * ║    "iat": 1710000000        (issued at = Unix timestamp)              ║
 * ║    "exp": 1710086400        (expires at = iat + 24h)                  ║
 * ║    "role": "ADMIN"          (custom claim we add)                     ║
 * ║                                                                       ║
 * ║  SIGNATURE: HMACSHA256(base64(header) + "." + base64(payload), key)  ║
 * ║    → Only our server can create valid signatures (knows the secret)   ║
 * ║    → Anyone can READ payload (it's base64, not encrypted!)           ║
 * ║    → But NO ONE can FAKE a token without our secret key               ║
 * ║                                                                       ║
 * ║  @Component → Registers as a Spring bean                              ║
 * ║  @Slf4j → Generates log field                                         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Component
@Slf4j
public class JwtUtil {

    /**
     * @Value("${app.jwt.secret}")
     *   → Injects value from application.properties
     *   → app.jwt.secret=pms_super_secret_key...
     *   → Spring replaces ${} with the actual value at startup
     */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    // ─────────────────────────────────────────────
    // TOKEN GENERATION
    // ─────────────────────────────────────────────

    /**
     * Generate JWT token for a user.
     *
     * Steps:
     * 1. Create claims map (payload data)
     * 2. Add subject (email), role, issued at, expiry
     * 3. Sign with HS256 algorithm and our secret key
     * 4. Compact to string format: "header.payload.signature"
     *
     * @param userDetails Spring Security's UserDetails object
     * @return JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Add role to token so we can authorize without DB lookup
        extraClaims.put("role", userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN"));
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                // setClaims → Add all extra claims (role, etc.)
                .setClaims(extraClaims)
                // setSubject → The "sub" claim = email (identifies the user)
                .setSubject(userDetails.getUsername())
                // setIssuedAt → The "iat" claim = current time
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // setExpiration → The "exp" claim = current time + 24 hours
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                // signWith → Create signature using HMAC-SHA256 + secret key
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                // compact → Serialize to "header.payload.signature" string
                .compact();
    }

    // ─────────────────────────────────────────────
    // TOKEN VALIDATION
    // ─────────────────────────────────────────────

    /**
     * Validate token:
     * 1. Extract username from token
     * 2. Check username matches the UserDetails
     * 3. Check token is not expired
     *
     * If signature is invalid → JwtException thrown (invalid token)
     * If expired → ExpiredJwtException thrown
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ─────────────────────────────────────────────
    // CLAIM EXTRACTION
    // ─────────────────────────────────────────────

    /**
     * Extract username (email) from token.
     * The "sub" claim holds the username.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Generic claim extractor using Function<Claims, T>.
     *
     * claimsResolver is a function that takes Claims and returns T.
     * Example: Claims::getSubject → extracts the "sub" field
     *
     * This is functional programming! We pass BEHAVIOR as parameter.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse the token and extract all claims.
     *
     * This is where JWT validation happens:
     * 1. Split "header.payload.signature"
     * 2. Decode header and payload from Base64
     * 3. Recompute signature from header + payload using our key
     * 4. Compare computed signature with token's signature
     * 5. If they match → token is authentic (not tampered with)
     * 6. If they don't match → throw SignatureException
     *
     * Possible exceptions:
     * - ExpiredJwtException → token.exp is in the past
     * - MalformedJwtException → token is not valid JWT format
     * - SignatureException → signature doesn't match (tampered!)
     * - UnsupportedJwtException → unsupported JWT type
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                // setSigningKey → The key to verify signature with
                .setSigningKey(getSigningKey())
                .build()
                // parseClaimsJws → Parse AND validate the token
                .parseClaimsJws(token)
                // getBody → Get the Claims (payload)
                .getBody();
    }

    /**
     * Convert the string secret to a cryptographic Key object.
     *
     * Keys.hmacShaKeyFor() creates an HMAC key.
     * The key must be at least 256 bits (32 bytes) for HS256.
     * Decoders.BASE64.decode() converts our string to bytes.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }
}
