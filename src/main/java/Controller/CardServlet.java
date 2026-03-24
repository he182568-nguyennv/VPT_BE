package Controller;

import Utils.DBConnection;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;

@WebServlet("/staff/cards")
public class CardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        Object roleAttr = req.getAttribute("jwtRoleId");
        if (roleAttr == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            return;
        }
        int roleId = (int) roleAttr;
        if (roleId != 1 && roleId != 2) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
            return;
        }

        String lotParam = req.getParameter("lotId");

        // ── Fix: dùng "status = 1" thay vì "is_active = 1"
        // Bảng cards có cột "status" (INTEGER), không có "is_active"
        String sql;
        if (lotParam != null && !lotParam.isEmpty()) {
            sql = "SELECT card_id, card_code, lot_id, card_type FROM cards " +
                    "WHERE lot_id = ? AND status = 1 " +
                    "AND card_id NOT IN (" +
                    "  SELECT card_id FROM parking_sessions WHERE status = 'active'" +
                    ") ORDER BY card_code";
        } else {
            sql = "SELECT card_id, card_code, lot_id, card_type FROM cards " +
                    "WHERE status = 1 " +
                    "AND card_id NOT IN (" +
                    "  SELECT card_id FROM parking_sessions WHERE status = 'active'" +
                    ") ORDER BY lot_id, card_code";
        }

        // ── Fix: dùng try-with-resources đúng cách để tránh resource leak
        try (Connection c  = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (lotParam != null && !lotParam.isEmpty()) {
                try {
                    ps.setInt(1, Integer.parseInt(lotParam));
                } catch (NumberFormatException e) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"success\":false,\"message\":\"lotId không hợp lệ\"}");
                    return;
                }
            }

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            try (ResultSet rs = ps.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{")
                            .append("\"cardId\":").append(rs.getInt("card_id")).append(",")
                            .append("\"cardCode\":\"").append(JsonUtil.escape(rs.getString("card_code"))).append("\",")
                            .append("\"lotId\":").append(rs.getInt("lot_id")).append(",")
                            .append("\"cardType\":\"").append(JsonUtil.escape(rs.getString("card_type"))).append("\"")
                            .append("}");
                    first = false;
                }
            }
            sb.append("]}");
            resp.setStatus(200);
            resp.getWriter().write(sb.toString());

        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" +
                    JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}