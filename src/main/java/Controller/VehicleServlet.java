package Controller;

import Dao.VehicleDAO;
import Model.RegisteredVehicle;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/customer/vehicles")
public class VehicleServlet extends HttpServlet {

    private final VehicleDAO vehicleDAO = new VehicleDAO();

    private String toJson(RegisteredVehicle v) {
        return "{" +
            "\"vehicleId\":"     + v.getVehicleId()                     + "," +
            "\"userId\":"        + v.getUserId()                        + "," +
            "\"typeId\":"        + v.getTypeId()                        + "," +
            "\"plateNumber\":\"" + JsonUtil.escape(v.getPlateNumber())  + "\"," +
            "\"isActive\":"      + v.isActive()                        +
            "}";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // ── Lấy attributes đã được AuthFilter set ──────────────
        Object userIdAttr = req.getAttribute("jwtUserId");
        Object roleIdAttr = req.getAttribute("jwtRoleId");

        // Guard: nếu AuthFilter chưa set (không nên xảy ra), trả 401
        if (userIdAttr == null || roleIdAttr == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized — token missing\"}");
            return;
        }

        int userId = (int) userIdAttr;
        int roleId = (int) roleIdAttr;

        // Xác định target: customer chỉ xem của mình, manager/staff có thể query userId param
        int targetId = userId;
        if (roleId != 3) {
            String param = req.getParameter("userId");
            if (param != null && !param.isBlank()) {
                try { targetId = Integer.parseInt(param); }
                catch (NumberFormatException e) {
                    resp.setStatus(400);
                    resp.getWriter().write("{\"success\":false,\"message\":\"userId không hợp lệ\"}");
                    return;
                }
            }
        }

        try {
            List<RegisteredVehicle> list = vehicleDAO.findByUser(targetId);
            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(list.get(i)));
            }
            sb.append("]}");
            resp.setStatus(200);
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        Object userIdAttr = req.getAttribute("jwtUserId");
        Object roleIdAttr = req.getAttribute("jwtRoleId");
        if (userIdAttr == null || roleIdAttr == null) {
            resp.setStatus(401); resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}"); return;
        }

        int userId = (int) userIdAttr;
        int roleId = (int) roleIdAttr;

        if (roleId != 3) {
            resp.setStatus(403);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ customer mới được quản lý xe\"}");
            return;
        }

        try {
            String body   = JsonUtil.readBody(req);
            String action = JsonUtil.getString(body, "action");

            if ("toggle".equals(action)) {
                int vehicleId = JsonUtil.getInt(body, "vehicleId", -1);
                if (vehicleId < 0) { resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing vehicleId\"}"); return; }
                boolean ok = vehicleDAO.toggleActive(vehicleId, userId);
                resp.setStatus(ok ? 200 : 404);
                resp.getWriter().write(ok ? "{\"success\":true}" : "{\"success\":false,\"message\":\"Không tìm thấy xe\"}");
                return;
            }

            if ("delete".equals(action)) {
                int vehicleId = JsonUtil.getInt(body, "vehicleId", -1);
                if (vehicleId < 0) { resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing vehicleId\"}"); return; }
                boolean ok = vehicleDAO.delete(vehicleId, userId);
                resp.setStatus(ok ? 200 : 404);
                resp.getWriter().write(ok ? "{\"success\":true}" : "{\"success\":false,\"message\":\"Không tìm thấy xe\"}");
                return;
            }

            String plate  = JsonUtil.getString(body, "plateNumber");
            int    typeId = JsonUtil.getInt(body, "typeId", 1);

            if (plate == null || plate.isBlank()) {
                resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu plateNumber\"}"); return;
            }
            if (vehicleDAO.findByPlate(plate) != null) {
                resp.setStatus(409); resp.getWriter().write("{\"success\":false,\"message\":\"Biển số đã tồn tại\"}"); return;
            }

            RegisteredVehicle v = new RegisteredVehicle();
            v.setUserId(userId); v.setTypeId(typeId);
            v.setPlateNumber(plate.toUpperCase().trim());
            int newId = vehicleDAO.insert(v);
            v.setVehicleId(newId); v.setActive(true);
            resp.setStatus(201);
            resp.getWriter().write("{\"success\":true,\"vehicle\":" + toJson(v) + "}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
