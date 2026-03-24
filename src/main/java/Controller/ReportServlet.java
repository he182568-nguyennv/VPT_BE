package Controller;

import Dao.ReportDAO;
import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Model.Report;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/reports")
public class ReportServlet extends HttpServlet {

    private final ReportDAO         reportDAO  = new ReportDAO();
    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        if (uid == null) { resp.setStatus(401); resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}"); return; }
        int userId = (int) uid;
        int roleId = (int) req.getAttribute("jwtRoleId");
        try {
            List<Report> list;
            if (roleId == 1)      list = reportDAO.findAll();
            else if (roleId == 2) list = reportDAO.findPending();
            else                  list = reportDAO.findByReporter(userId);

            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(enrichedJson(list.get(i)));
            }
            sb.append("]}");
            resp.setStatus(200); resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        if (uid == null) { resp.setStatus(401); resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}"); return; }
        int userId = (int) uid;
        int roleId = (int) req.getAttribute("jwtRoleId");
        String jwtUsername = (String) req.getAttribute("jwtUsername");
        try {
            String body   = JsonUtil.readBody(req);
            String action = JsonUtil.getString(body, "action");
            if ("approve".equals(action)) {
                if (roleId != 1) { resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return; }
                int    reportId = JsonUtil.getInt(body, "reportId", -1);
                String decision = JsonUtil.getString(body, "decision");
                String note     = JsonUtil.getString(body, "note");
                if (reportId < 0 || decision == null) { resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing fields\"}"); return; }
                reportDAO.approve(reportId, userId, decision, note != null ? note : "");
                resp.setStatus(200); resp.getWriter().write("{\"success\":true,\"reportId\":" + reportId + "}");
                return;
            }
            int    vehicleId    = JsonUtil.getInt(body, "vehicleId", 0);
            int    sessionId    = JsonUtil.getInt(body, "sessionId", -1);
            String reportType   = JsonUtil.getString(body, "reportType");
            String notes        = JsonUtil.getString(body, "notes");
            String reporterName = JsonUtil.getString(body, "reporterName");
            String reporterPhone= JsonUtil.getString(body, "reporterPhone");
            if (sessionId < 0 || reportType == null) { resp.setStatus(400); resp.getWriter().write("{\"success\":false,\"message\":\"Missing sessionId or reportType\"}"); return; }
            if (reporterName == null || reporterName.isBlank()) reporterName = jwtUsername != null ? jwtUsername : "Unknown";
            Report r = new Report();
            r.setReporterId(userId); r.setVehicleId(vehicleId); r.setSessionId(sessionId);
            r.setReporterName(reporterName); r.setReporterPhone(reporterPhone != null ? reporterPhone : "");
            r.setReportType(reportType); r.setNotes(notes != null ? notes : "");
            int newId = reportDAO.insert(r);
            resp.setStatus(201); resp.getWriter().write("{\"success\":true,\"reportId\":" + newId + "}");
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }

    private String enrichedJson(Report r) {
        String plate = "", checkinTime = "";
        try {
            ParkingSession s = sessionDAO.findById(r.getSessionId());
            if (s != null) { plate = s.getPlateNumber() != null ? s.getPlateNumber() : ""; checkinTime = s.getCheckinTime() != null ? s.getCheckinTime() : ""; }
        } catch (Exception ignored) {}
        return "{\"reportId\":" + r.getReportId()
            + ",\"reporterId\":" + r.getReporterId()
            + ",\"vehicleId\":"  + r.getVehicleId()
            + ",\"sessionId\":"  + r.getSessionId()
            + ",\"plateNumber\":\"" + JsonUtil.escape(plate) + "\""
            + ",\"checkinTime\":\"" + JsonUtil.escape(checkinTime) + "\""
            + ",\"reporterName\":\"" + JsonUtil.escape(r.getReporterName()) + "\""
            + ",\"reporterPhone\":\"" + JsonUtil.escape(r.getReporterPhone()) + "\""
            + ",\"reportType\":\"" + JsonUtil.escape(r.getReportType()) + "\""
            + ",\"notes\":\"" + JsonUtil.escape(r.getNotes()) + "\""
            + ",\"status\":\"" + JsonUtil.escape(r.getStatus()) + "\""
            + ",\"createdAt\":\"" + JsonUtil.escape(r.getCreatedAt()) + "\""
            + "}";
    }
}
