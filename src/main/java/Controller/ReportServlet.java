package Controller;

import Dao.ReportDAO;
import Model.Report;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private final ReportDAO reportDAO = new ReportDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int userId = (int) req.getAttribute("jwtUserId");
        int roleId = (int) req.getAttribute("jwtRoleId");

        try {
            List<Report> reports = (roleId == 1)
                    ? reportDAO.findPending()
                    : reportDAO.findByReporter(userId);

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < reports.size(); i++) {
                Report r = reports.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"reportId\":").append(r.getReportId()).append(",")
                        .append("\"reporterId\":").append(r.getReporterId()).append(",")
                        .append("\"vehicleId\":").append(r.getVehicleId()).append(",")
                        .append("\"sessionId\":").append(r.getSessionId()).append(",")
                        .append("\"reporterName\":\"").append(JsonUtil.escape(r.getReporterName())).append("\",")
                        .append("\"reporterPhone\":\"").append(JsonUtil.escape(r.getReporterPhone())).append("\",")
                        .append("\"reportType\":\"").append(JsonUtil.escape(r.getReportType())).append("\",")
                        .append("\"notes\":\"").append(JsonUtil.escape(r.getNotes())).append("\",")
                        .append("\"status\":\"").append(JsonUtil.escape(r.getStatus())).append("\",")
                        .append("\"createdAt\":\"").append(JsonUtil.escape(r.getCreatedAt())).append("\"")
                        .append("}");
            }
            sb.append("]}");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        int userId = (int) req.getAttribute("jwtUserId");
        int roleId = (int) req.getAttribute("jwtRoleId");

        try {
            String body   = JsonUtil.readBody(req);
            String action = JsonUtil.getString(body, "action");

            if ("create".equals(action)) {
                Report r = new Report();
                r.setReporterId(userId);
                r.setVehicleId(JsonUtil.getInt(body, "vehicleId", 0));
                r.setSessionId(JsonUtil.getInt(body, "sessionId", 0));
                r.setReportType(JsonUtil.getString(body, "reportType"));
                r.setNotes(JsonUtil.getString(body, "notes"));
                int id = reportDAO.insert(r);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"success\":true,\"reportId\":" + id + ",\"status\":\"pending\"}");

            } else if ("approve".equals(action)) {
                if (roleId != 1) {
                    resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ manager mới được duyệt\"}");
                    return;
                }
                int    reportId = JsonUtil.getInt(body, "reportId", -1);
                String decision = JsonUtil.getString(body, "decision");
                String note     = JsonUtil.getString(body, "note");
                if (reportId < 0 || decision == null) {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu reportId hoặc decision\"}");
                    return;
                }
                reportDAO.approve(reportId, userId, decision, note);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"success\":true,\"reportId\":" + reportId + ",\"decision\":\"" + JsonUtil.escape(decision) + "\"}");

            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Action không hợp lệ\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}