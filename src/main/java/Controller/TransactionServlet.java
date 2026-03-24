package Controller;

import Dao.TransactionDAO;
import Model.Transaction;
import Utils.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/transactions")
public class TransactionServlet extends HttpServlet {

    private final TransactionDAO dao = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Object uid = req.getAttribute("jwtUserId");
        Object rid = req.getAttribute("jwtRoleId");
        if (uid == null) { resp.setStatus(401); resp.getWriter().write("{\"success\":false,\"message\":\"Unauthorized\"}"); return; }
        int userId = (int) uid;
        int roleId = (int) rid;
        try {
            List<Transaction> list;
            if (roleId == 1) {
                String lotParam  = req.getParameter("lotId");
                String fromParam = req.getParameter("from");
                String toParam   = req.getParameter("to");
                if (fromParam != null && toParam != null) {
                    int lotId = lotParam != null ? Integer.parseInt(lotParam) : 0;
                    list = dao.findByRange(lotId, fromParam, toParam);
                } else {
                    list = dao.findAll();
                }
            } else if (roleId == 2) {
                list = dao.findByStaff(userId);
            } else if (roleId == 3) {
                list = dao.findByUser(userId);
            } else {
                resp.setStatus(403); resp.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}"); return;
            }
            StringBuilder sb = new StringBuilder("{\"success\":true,\"data\":[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                Transaction t = list.get(i);
                sb.append("{\"transId\":").append(t.getTransId())
                  .append(",\"sessionId\":").append(t.getSessionId())
                  .append(",\"amount\":").append(t.getAmount())
                  .append(",\"discountAmount\":").append(t.getDiscountAmount())
                  .append(",\"paymentMethod\":\"").append(JsonUtil.escape(t.getPaymentMethod())).append("\"")
                  .append(",\"feeType\":\"").append(JsonUtil.escape(t.getFeeType())).append("\"")
                  .append(",\"paymentStatus\":\"").append(JsonUtil.escape(t.getPaymentStatus())).append("\"")
                  .append(",\"createdAt\":\"").append(JsonUtil.escape(t.getCreatedAt())).append("\"")
                  .append("}");
            }
            sb.append("]}");
            resp.setStatus(200); resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(500); resp.getWriter().write("{\"success\":false,\"message\":\"" + JsonUtil.escape(e.getMessage()) + "\"}");
        }
    }
}
