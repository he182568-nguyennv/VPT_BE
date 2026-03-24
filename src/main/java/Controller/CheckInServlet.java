package Controller;

import Dao.ParkingSessionDAO;
import Model.ParkingSession;
import Utils.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/staff/checkin")
public class CheckInServlet extends HttpServlet {

    private final ParkingSessionDAO sessionDAO = new ParkingSessionDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        // FIX #4: null-check trước khi cast để tránh NullPointerException
        Object roleAttr = req.getAttribute("jwtRoleId");
        Object userAttr = req.getAttribute("jwtUserId");
        if (roleAttr == null || userAttr == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}");
            return;
        }

        int roleId  = (int) roleAttr;
        int staffId = (int) userAttr;

        if (roleId != 2) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.getWriter().write("{\"success\":false,\"message\":\"Chỉ staff mới được check-in\"}");
            return;
        }

        try {
            String body        = JsonUtil.readBody(req);
            String plateNumber = JsonUtil.getString(body, "plateNumber");
            int    lotId       = JsonUtil.getInt(body, "lotId", -1);
            int    cardId      = JsonUtil.getInt(body, "cardId", -1);
            String imgIn       = JsonUtil.getString(body, "vehicleImgIn");

            // FIX #1: parse thêm vehicleId và membershipId từ request body
            int    vehicleId    = JsonUtil.getInt(body, "vehicleId", 0);
            int    membershipId = JsonUtil.getInt(body, "membershipId", 0);

            if (plateNumber == null || plateNumber.isBlank() || lotId < 0 || cardId < 0) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"success\":false,\"message\":\"Thiếu plateNumber, lotId hoặc cardId\"}");
                return;
            }

            // Chuẩn hóa biển số về chữ hoa
            plateNumber = plateNumber.trim().toUpperCase();

            ParkingSession existing = sessionDAO.findActiveByPlate(plateNumber);
            if (existing != null) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"success\":false,\"message\":\"Xe " +
                        JsonUtil.escape(plateNumber) + " đang trong bãi rồi\"}");
                return;
            }

            // FIX #1: set đầy đủ vehicleId và membershipId vào session object
            ParkingSession s = new ParkingSession();
            s.setPlateNumber(plateNumber);
            s.setLotId(lotId);
            s.setCardId(cardId);
            s.setStaffCheckinId(staffId);
            s.setVehicleImgIn(imgIn != null ? imgIn : "");
            s.setVehicleId(vehicleId);       // trước đây bị bỏ sót → vehicle_id = 0
            s.setMembershipId(membershipId); // trước đây bị bỏ sót → membership_id = 0

            int sid = sessionDAO.checkIn(s);
            if (sid < 0) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"success\":false,\"message\":\"Check-in thất bại, không lấy được session ID\"}");
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write(
                    "{\"success\":true," +
                            "\"sessionId\":"     + sid                                          + "," +
                            "\"plateNumber\":\"" + JsonUtil.escape(plateNumber)                 + "\"," +
                            "\"lotId\":"         + lotId                                        + "," +
                            "\"cardId\":"        + cardId                                       + "," +
                            "\"vehicleId\":"     + vehicleId                                    + "," +
                            "\"membershipId\":"  + membershipId                                 + "," +
                            "\"staffCheckinId\":" + staffId                                     + "," +
                            "\"checkinTime\":\"" + java.time.LocalDateTime.now()               + "\"," +
                            "\"status\":\"active\"}"
            );

        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"success\":false,\"message\":\"Lỗi server: " +
                    JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}