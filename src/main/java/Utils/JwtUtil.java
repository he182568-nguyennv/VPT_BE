package Utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Minimal JWT HS256 — không cần thư viện ngoài.
 * Production nên dùng java-jwt hoặc jjwt.
 */
public class JwtUtil {

    private static final String SECRET     = "vpt-super-secret-key-2024";
    private static final long   EXPIRY_MS  = 24 * 60 * 60 * 1000L; // 24h

    public static String generateToken(int userId, String username, int roleId) {
        String header  = b64("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = b64("{\"userId\":" + userId +
                ",\"username\":\"" + username + "\"" +
                ",\"roleId\":" + roleId +
                ",\"exp\":" + (System.currentTimeMillis() + EXPIRY_MS) + "}");
        String sig = sign(header + "." + payload);
        return header + "." + payload + "." + sig;
    }

    public static boolean isValid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            long exp = getExpiry(parts[1]);
            if (exp < System.currentTimeMillis()) return false;
            return sign(parts[0] + "." + parts[1]).equals(parts[2]);
        } catch (Exception e) { return false; }
    }

    public static int getUserId(String token) {
        return getIntClaim(token, "userId");
    }

    public static int getRoleId(String token) {
        return getIntClaim(token, "roleId");
    }

    public static String getUsername(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            String key = "\"username\":\"";
            int s = payload.indexOf(key) + key.length();
            int e = payload.indexOf("\"", s);
            return payload.substring(s, e);
        } catch (Exception ex) { return null; }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static long getExpiry(String b64Payload) {
        String payload = new String(Base64.getUrlDecoder().decode(b64Payload));
        String key = "\"exp\":";
        int s = payload.indexOf(key) + key.length();
        int e = payload.indexOf("}", s);
        if (e < 0) e = payload.indexOf(",", s);
        return Long.parseLong(payload.substring(s, e).trim());
    }

    private static int getIntClaim(String token, String claimKey) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            String key = "\"" + claimKey + "\":";
            int s = payload.indexOf(key) + key.length();
            int e = payload.indexOf(",", s);
            if (e < 0) e = payload.indexOf("}", s);
            return Integer.parseInt(payload.substring(s, e).trim());
        } catch (Exception ex) { return -1; }
    }
}