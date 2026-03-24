package Filter;

import Utils.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * AuthFilter — chạy trước MỌI request (/*).
 *
 * FIX #4: getAttribute() ở servlet có thể trả về null nếu filter bị bypass
 * hoặc token parse fail. Filter này đảm bảo luôn set đủ 3 attribute trước
 * khi chain.doFilter() — servlet chỉ cần null-check là đủ.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/login",
            "/logout",
            "/payment/vnpay-return",
            "/payment/vnpay-ipn"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path   = request.getServletPath();
        String method = request.getMethod();

        // Bỏ qua OPTIONS (CORS preflight) và các path public
        if ("OPTIONS".equalsIgnoreCase(method) || isPublicPath(path)) {
            chain.doFilter(req, res);
            return;
        }

        // Lấy token từ header Authorization: Bearer <token>
        String token = extractToken(request);

        if (token == null || !JwtUtil.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Token không hợp lệ hoặc đã hết hạn\"}"
            );
            return;
        }

        int userId   = JwtUtil.getUserId(token);
        int roleId   = JwtUtil.getRoleId(token);
        String uname = JwtUtil.getUsername(token);

        // Nếu parse token ra -1 nghĩa là token bị lỗi dù đã pass isValid()
        // (edge case: token hợp lệ nhưng thiếu claim) → từ chối luôn
        if (userId < 0 || roleId < 0) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Token thiếu thông tin người dùng\"}"
            );
            return;
        }

        // Set attributes để servlet dùng — PHẢI set trước chain.doFilter()
        request.setAttribute("jwtUserId",   userId);
        request.setAttribute("jwtRoleId",   roleId);
        request.setAttribute("jwtUsername", uname);

        chain.doFilter(req, res);
    }

    // Hỗ trợ cả exact match và prefix match (ví dụ /public/*)
    private boolean isPublicPath(String path) {
        for (String p : PUBLIC_PATHS) {
            if (path.equals(p) || path.startsWith(p + "/")) return true;
        }
        return false;
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    @Override public void init(FilterConfig cfg) {}
    @Override public void destroy() {}
}