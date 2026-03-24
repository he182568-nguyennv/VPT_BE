package Filter;

import Utils.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * AuthFilter — chạy trước MỌI servlet (/*).
 *
 * FIX chính: set 3 attribute jwtUserId, jwtRoleId, jwtUsername vào request
 * trước khi chuyển sang servlet. Nếu thiếu bước này, servlet sẽ bị
 * NullPointerException khi cast getAttribute() về int.
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
        if ("OPTIONS".equalsIgnoreCase(method) || PUBLIC_PATHS.contains(path)) {
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

        // ── Set attributes để servlet dùng ──────────────────────
        // Đây là phần quan trọng — PHẢI set trước chain.doFilter()
        request.setAttribute("jwtUserId",   JwtUtil.getUserId(token));
        request.setAttribute("jwtRoleId",   JwtUtil.getRoleId(token));
        request.setAttribute("jwtUsername", JwtUtil.getUsername(token));

        chain.doFilter(req, res);
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
