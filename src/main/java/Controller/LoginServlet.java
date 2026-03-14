package Controller;


import Dao.UserDAO;
import Model.User;
import Utils.JsonUtil;
import Utils.JwtUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.MessageDigest;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String body     = JsonUtil.readBody(req);
        String username = JsonUtil.getString(body, "username");
        String password = JsonUtil.getString(body, "password");

        if (username == null || password == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu username hoặc password\"}");
            return;
        }

        try {
            User user = userDAO.findByUsername(username);
            if (user != null && user.isActive() && checkPassword(password, user.getPasswordHash())) {
                String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleId());
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write(
                        "{\"success\":true," +
                                "\"token\":\"" + token + "\"," +
                                "\"userId\":" + user.getId() + "," +
                                "\"username\":\"" + JsonUtil.escape(user.getUsername()) + "\"," +
                                "\"fullName\":\"" + JsonUtil.escape(user.getFullName()) + "\"," +
                                "\"roleId\":" + user.getRoleId() + "," +
                                "\"role\":\"" + getRoleName(user.getRoleId()) + "\"}"
                );
            } else {
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                resp.getWriter().write("{\"success\":false,\"message\":\"Sai tên đăng nhập hoặc mật khẩu\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Lỗi server\"}");
        }
    }

    private boolean checkPassword(String raw, String hash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString().equals(hash);
        } catch (Exception e) { return false; }
    }

    private String getRoleName(int roleId) {
        switch (roleId) {
            case 1: return "manager";
            case 2: return "staff";
            case 3: return "customer";
            default: return "unknown";
        }
    }
}