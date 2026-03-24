package Controller;

import Dao.MembershipDAO;
import Model.Membership;
import Model.MembershipPlan;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@WebServlet("/customer/membership")
public class MembershipServlet extends HttpServlet {

    private final MembershipDAO dao = new MembershipDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { resp.setStatus(401); resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}"); return; }
        int userId = (int) uid;
        int roleId = (int) rid;
        if (roleId != 3 && roleId != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }
        if (roleId == 1) {
            String p = req.getParameter("userId");
            if (p != null && !p.isBlank()) try { userId = Integer.parseInt(p); } catch (Exception ignored) {}
        }
        try {
            List<MembershipPlan> plans = dao.findAllPlans();
            Membership active          = dao.findActiveByUser(userId);

            StringBuilder plansJson = new StringBuilder("[");
            for (int i = 0; i < plans.size(); i++) {
                MembershipPlan p = plans.get(i);
                if (i > 0) plansJson.append(",");
                plansJson.append("{")
                    .append("\"planId\":").append(p.getPlanId()).append(",")
                    .append("\"name\":\"").append(JsonUtil.escape(p.getName())).append("\",")
                    .append("\"durationDays\":").append(p.getDurationDays()).append(",")
                    .append("\"price\":").append(p.getPrice()).append(",")
                    .append("\"discountPct\":").append(p.getDiscountPct())
                    .append("}");
            }
            plansJson.append("]");

            String activeJson = "null";
            if (active != null) {
                long daysLeft = 0;
                try { daysLeft = Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(active.getEndDate()))); } catch (Exception ignored) {}
                MembershipPlan plan = dao.getPlanById(active.getPlanId());
                String planName = plan != null ? plan.getName() : "";
                activeJson = "{\"membershipId\":" + active.getMembershipId()
                    + ",\"planId\":"     + active.getPlanId()
                    + ",\"planName\":\"" + JsonUtil.escape(planName) + "\""
                    + ",\"startDate\":\"" + JsonUtil.escape(active.getStartDate()) + "\""
                    + ",\"endDate\":\""  + JsonUtil.escape(active.getEndDate())   + "\""
                    + ",\"status\":\""   + JsonUtil.escape(active.getStatus())    + "\""
                    + ",\"daysLeft\":"   + daysLeft + "}";
            }
            resp.setStatus(200);
            resp.getWriter().write("{\"success\":true,\"plans\":" + plansJson + ",\"active\":" + activeJson + "}");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { resp.setStatus(401); resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}"); return; }
        int userId = (int) uid;
        if ((int) rid != 3) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ customer mới được đăng ký\"}"); return; }
        try {
            String body = JsonUtil.readBody(req);
            int planId  = JsonUtil.getInt(body, "planId", -1);
            if (planId < 0) { resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu planId\"}"); return; }
            Membership ex = dao.findActiveByUser(userId);
            if (ex != null) { resp.setStatus(409); resp.getWriter().write("{\"success\":false,\"message\":\"Đang có gói active đến " + ex.getEndDate() + "\"}"); return; }
            int newId = dao.register(userId, planId);
            if (newId < 0) { resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Gói không tồn tại\"}"); return; }
            resp.setStatus(201);
            resp.getWriter().write("{\"success\":true,\"membershipId\":" + newId + "}");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
