package Filter;

import Utils.JwtUtil;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@WebFilter({"/staff/*", "/manager/*", "/reports"})
public class AuthFilter implements Filter {

    private static final List<String> PUBLIC = Arrays.asList("/login", "/logout");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest)  req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getServletPath();
        if (PUBLIC.contains(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String token = extractToken(request);
        if (token == null || !JwtUtil.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Token không hợp lệ hoặc đã hết hạn\"}");
            return;
        }

        // Truyền thông tin user vào request attribute để servlet dùng
        request.setAttribute("jwtUserId",  JwtUtil.getUserId(token));
        request.setAttribute("jwtRoleId",  JwtUtil.getRoleId(token));
        request.setAttribute("jwtUsername",JwtUtil.getUsername(token));

        chain.doFilter(req, res);
    }

    private String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return header.substring(7);
        return null;
    }
}