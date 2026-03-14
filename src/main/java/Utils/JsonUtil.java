package Utils;

import java.io.BufferedReader;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Đọc JSON body từ request và escape string an toàn.
 * Không cần Gson/Jackson — đủ dùng cho project nhỏ.
 */
public class JsonUtil {

    /** Đọc toàn bộ body của request thành String */
    public static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    /** Lấy giá trị string từ JSON đơn giản: {"key":"value"} */
    public static String getString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int s = json.indexOf(search);
        if (s < 0) return null;
        s += search.length();
        int e = json.indexOf("\"", s);
        return e < 0 ? null : json.substring(s, e);
    }

    /** Lấy giá trị number từ JSON đơn giản: {"key":123} */
    public static int getInt(String json, String key, int defaultVal) {
        String search = "\"" + key + "\":";
        int s = json.indexOf(search);
        if (s < 0) return defaultVal;
        s += search.length();
        int e = s;
        while (e < json.length() && (Character.isDigit(json.charAt(e)) || json.charAt(e) == '-')) e++;
        try { return Integer.parseInt(json.substring(s, e)); }
        catch (NumberFormatException ex) { return defaultVal; }
    }

    /** Escape ký tự đặc biệt trong string để tránh JSON injection */
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}