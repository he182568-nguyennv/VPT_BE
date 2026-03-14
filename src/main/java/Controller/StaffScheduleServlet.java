package Controller;

import Dao.StaffScheduleDAO;
import Model.StaffSchedule;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/staff/schedule")
public class StaffScheduleServlet extends HttpServlet {

    private final StaffScheduleDAO scheduleDAO = new StaffScheduleDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int roleId = (int) req.getAttribute("jwtRoleId");
        int staffId = (int) req.getAttribute("jwtUserId");
        if (roleId != 2) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ staff mới được xem lịch\"}");
            return;
        }

        try {
            List<StaffSchedule> schedules = scheduleDAO.findByStaff(staffId);
            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < schedules.size(); i++) {
                StaffSchedule s = schedules.get(i);
                if (i > 0) sb.append(",");
                sb.append("{")
                        .append("\"scheduleId\":").append(s.getScheduleId()).append(",")
                        .append("\"staffId\":").append(s.getStaffId()).append(",")
                        .append("\"lotId\":").append(s.getLotId()).append(",")
                        .append("\"workDate\":\"").append(JsonUtil.escape(s.getWorkDate())).append("\",")
                        .append("\"shiftStart\":\"").append(JsonUtil.escape(s.getShiftStart())).append("\",")
                        .append("\"shiftEnd\":\"").append(JsonUtil.escape(s.getShiftEnd())).append("\",")
                        .append("\"status\":\"").append(JsonUtil.escape(s.getStatus())).append("\"")
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
}