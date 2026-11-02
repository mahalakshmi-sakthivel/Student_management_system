package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dao.AdminDAO;
import dao.DepartmentDAO;
import dao.StudentDAO;
import model.Department;
import model.Student;
import util.Validator;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP Web Server for the Student Management System.
 * Serves the Wine Red & Black Web application on port 8080.
 */
public class WebServer {

    private final int port;
    private HttpServer server;
    private final StudentDAO studentDAO = new StudentDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    public WebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // Handlers
        server.createContext("/", new StaticAppHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/departments", new DepartmentsHandler());
        server.createContext("/api/students", new StudentsHandler());

        server.start();
        System.out.println("==================================================================");
        System.out.println("  Student Management System Web Server Active!                    ");
        System.out.println("  Open in Browser: http://localhost:" + port + "                   ");
        System.out.println("  Default Admin: admin / admin123                                 ");
        System.out.println("==================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public static void main(String[] args) throws IOException {
        int p = 8080;
        if (args.length > 0) {
            try { p = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        WebServer ws = new WebServer(p);
        ws.start();
    }

    // ------------------------------------------------------------------------
    // API Handlers
    // ------------------------------------------------------------------------

    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                sendResponse(ex, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }
            Map<String, String> body = parseJsonOrForm(readBody(ex));
            String user = body.get("username");
            String pass = body.get("password");

            try {
                boolean ok = adminDAO.authenticate(user, pass);
                if (ok) {
                    sendResponse(ex, 200, "{\"success\":true,\"username\":\"" + escapeJson(user) + "\"}");
                } else {
                    sendResponse(ex, 401, "{\"success\":false,\"error\":\"Invalid username or password.\"}");
                }
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"success\":false,\"error\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
                int totalStudents = studentDAO.getStudentCount();
                int totalDepts = departmentDAO.getAllDepartments().size();
                Map<String, Integer> genderMap = studentDAO.getGenderCounts();

                String json = "{"
                    + "\"totalStudents\":" + totalStudents + ","
                    + "\"totalDepartments\":" + totalDepts + ","
                    + "\"maleCount\":" + genderMap.getOrDefault("Male", 0) + ","
                    + "\"femaleCount\":" + genderMap.getOrDefault("Female", 0) + ","
                    + "\"otherCount\":" + genderMap.getOrDefault("Other", 0)
                    + "}";
                sendResponse(ex, 200, json);
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class DepartmentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
                List<Department> list = departmentDAO.getAllDepartments();
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    Department d = list.get(i);
                    sb.append("{\"id\":").append(d.getId())
                      .append(",\"name\":\"").append(escapeJson(d.getName())).append("\"}");
                    if (i < list.size() - 1) sb.append(",");
                }
                sb.append("]");
                sendResponse(ex, 200, sb.toString());
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod().toUpperCase();
            try {
                switch (method) {
                    case "GET":
                        handleGet(ex);
                        break;
                    case "POST":
                        handlePost(ex);
                        break;
                    case "PUT":
                        handlePut(ex);
                        break;
                    case "DELETE":
                        handleDelete(ex);
                        break;
                    default:
                        sendResponse(ex, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                sendResponse(ex, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }

        private void handleGet(HttpExchange ex) throws Exception {
            String query = ex.getRequestURI().getQuery();
            String q = "";
            if (query != null && query.startsWith("q=")) {
                q = URLDecoder.decode(query.substring(2), StandardCharsets.UTF_8);
            }

            List<Student> list = q.isEmpty() ? studentDAO.getAllStudents() : studentDAO.searchStudents(q);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Student s = list.get(i);
                sb.append("{")
                  .append("\"id\":").append(s.getId()).append(",")
                  .append("\"name\":\"").append(escapeJson(s.getName())).append("\",")
                  .append("\"dob\":\"").append(s.getDob() != null ? s.getDob().toString() : "").append("\",")
                  .append("\"gender\":\"").append(escapeJson(s.getGender())).append("\",")
                  .append("\"departmentId\":").append(s.getDepartmentId()).append(",")
                  .append("\"departmentName\":\"").append(escapeJson(s.getDepartmentName() != null ? s.getDepartmentName() : "")).append("\",")
                  .append("\"email\":\"").append(escapeJson(s.getEmail())).append("\",")
                  .append("\"phone\":\"").append(escapeJson(s.getPhone())).append("\"")
                  .append("}");
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            sendResponse(ex, 200, sb.toString());
        }

        private void handlePost(HttpExchange ex) throws Exception {
            Map<String, String> b = parseJsonOrForm(readBody(ex));
            String idStr = b.get("id");
            String name = b.get("name");
            String dobStr = b.get("dob");
            String gender = b.get("gender");
            String deptIdStr = b.get("departmentId");
            String email = b.get("email");
            String phone = b.get("phone");

            boolean deptSelected = deptIdStr != null && !deptIdStr.isEmpty() && !deptIdStr.equals("0");
            Validator.ValidationResult val = Validator.validateStudent(idStr, name, dobStr, gender, email, phone, deptSelected);
            if (!val.isValid()) {
                sendResponse(ex, 400, "{\"error\":\"" + escapeJson(val.getFormattedErrors()) + "\"}");
                return;
            }

            int id = Integer.parseInt(idStr);
            if (studentDAO.isStudentIdTaken(id)) {
                sendResponse(ex, 400, "{\"error\":\"Student ID " + id + " already exists.\"}");
                return;
            }
            if (studentDAO.isEmailTaken(email, -1)) {
                sendResponse(ex, 400, "{\"error\":\"Email is already registered.\"}");
                return;
            }

            int deptId = Integer.parseInt(deptIdStr);
            Department dept = departmentDAO.getDepartmentById(deptId);
            String deptName = (dept != null) ? dept.getName() : "";

            Student s = new Student(id, name, Validator.parseDate(dobStr), gender, email, phone, deptId, deptName);
            studentDAO.addStudent(s);
            sendResponse(ex, 201, "{\"success\":true,\"message\":\"Student added successfully\"}");
        }

        private void handlePut(HttpExchange ex) throws Exception {
            Map<String, String> b = parseJsonOrForm(readBody(ex));
            String idStr = b.get("id");
            String name = b.get("name");
            String dobStr = b.get("dob");
            String gender = b.get("gender");
            String deptIdStr = b.get("departmentId");
            String email = b.get("email");
            String phone = b.get("phone");

            boolean deptSelected = deptIdStr != null && !deptIdStr.isEmpty() && !deptIdStr.equals("0");
            Validator.ValidationResult val = Validator.validateStudent(idStr, name, dobStr, gender, email, phone, deptSelected);
            if (!val.isValid()) {
                sendResponse(ex, 400, "{\"error\":\"" + escapeJson(val.getFormattedErrors()) + "\"}");
                return;
            }

            int id = Integer.parseInt(idStr);
            if (studentDAO.isEmailTaken(email, id)) {
                sendResponse(ex, 400, "{\"error\":\"Email is already registered to another student.\"}");
                return;
            }

            int deptId = Integer.parseInt(deptIdStr);
            Department dept = departmentDAO.getDepartmentById(deptId);
            String deptName = (dept != null) ? dept.getName() : "";

            Student s = new Student(id, name, Validator.parseDate(dobStr), gender, email, phone, deptId, deptName);
            studentDAO.updateStudent(s);
            sendResponse(ex, 200, "{\"success\":true,\"message\":\"Student updated successfully\"}");
        }

        private void handleDelete(HttpExchange ex) throws Exception {
            String query = ex.getRequestURI().getQuery();
            if (query == null || !query.startsWith("id=")) {
                sendResponse(ex, 400, "{\"error\":\"Missing student id query parameter\"}");
                return;
            }
            int id = Integer.parseInt(query.substring(3).trim());
            boolean ok = studentDAO.deleteStudent(id);
            if (ok) {
                sendResponse(ex, 200, "{\"success\":true,\"message\":\"Student deleted successfully\"}");
            } else {
                sendResponse(ex, 404, "{\"error\":\"Student not found\"}");
            }
        }
    }

    // ------------------------------------------------------------------------
    // Static HTML/CSS/JS Handler (Single Page App in Wine & Black)
    // ------------------------------------------------------------------------

    private class StaticAppHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String html = getAppHtml();
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void sendResponse(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while ((n = is.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseJsonOrForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            // Simple JSON parsing without external library
            String inner = body.substring(1, body.length() - 1);
            String[] pairs = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                if (kv.length == 2) {
                    String k = cleanString(kv[0]);
                    String v = cleanString(kv[1]);
                    map.put(k, v);
                }
            }
        } else {
            // Form URL encoded
            String[] pairs = body.split("&");
            for (String p : pairs) {
                String[] kv = p.split("=", 2);
                if (kv.length == 2) {
                    map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    private String cleanString(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String getAppHtml() {
        return "<!DOCTYPE html>\n" +
"<html lang=\"en\">\n" +
"<head>\n" +
"  <meta charset=\"UTF-8\" />\n" +
"  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
"  <title>Student Management System</title>\n" +
"  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n" +
"  <link href=\"https://fonts.googleapis.com/css2?family=Segoe+UI:wght@400;600;700&display=swap\" rel=\"stylesheet\">\n" +
"  <style>\n" +
"    :root {\n" +
"      --wine: #722F37;\n" +
"      --wine-dark: #4A0E1A;\n" +
"      --wine-light: #9B3A47;\n" +
"      --black: #0D0D0D;\n" +
"      --charcoal: #1A1A1A;\n" +
"      --charcoal-light: #222222;\n" +
"      --field-bg: #262626;\n" +
"      --text: #F5F0F0;\n" +
"      --text-muted: #A8A0A0;\n" +
"      --accent: #C9A27E;\n" +
"      --success: #4CAF6A;\n" +
"      --error: #E0434F;\n" +
"    }\n" +
"    * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
"    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: var(--black); color: var(--text); height: 100vh; overflow: hidden; }\n" +
"    \n" +
"    /* Login View */\n" +
"    #loginView { display: flex; align-items: center; justify-content: center; height: 100vh; background: var(--black); }\n" +
"    .login-card {\n" +
"      background: var(--charcoal); width: 420px; padding: 40px 36px; border-radius: 12px;\n" +
"      border: 1px solid #333; box-shadow: 0 16px 40px rgba(0,0,0,0.8); text-align: center;\n" +
"    }\n" +
"    .logo { font-size: 54px; margin-bottom: 8px; }\n" +
"    .login-title { font-size: 24px; font-weight: 700; color: var(--accent); letter-spacing: 1px; }\n" +
"    .login-sub { font-size: 14px; color: var(--text-muted); margin-bottom: 24px; }\n" +
"    .form-group { text-align: left; margin-bottom: 18px; }\n" +
"    .form-group label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; color: var(--text); }\n" +
"    .input-ctrl {\n" +
"      width: 100%; background: var(--field-bg); border: 1px solid #444; border-radius: 6px;\n" +
"      padding: 10px 14px; font-size: 14px; color: var(--text); outline: none; transition: border 0.2s;\n" +
"    }\n" +
"    .input-ctrl:focus { border-color: var(--wine); box-shadow: 0 0 0 2px rgba(114,47,55,0.4); }\n" +
"    .btn-primary {\n" +
"      width: 100%; background: var(--wine); color: #FFF; border: none; padding: 12px; border-radius: 6px;\n" +
"      font-size: 14px; font-weight: 700; cursor: pointer; transition: background 0.2s; margin-top: 10px;\n" +
"    }\n" +
"    .btn-primary:hover { background: var(--wine-light); }\n" +
"    .demo-pill {\n" +
"      background: #2a1a1e; border: 1px dashed var(--accent); color: var(--accent); padding: 8px 12px;\n" +
"      border-radius: 6px; font-size: 12px; margin-top: 18px; cursor: pointer; transition: 0.2s;\n" +
"    }\n" +
"    .demo-pill:hover { background: #3c2027; }\n" +
"    .alert-banner { color: var(--error); font-size: 13px; margin-bottom: 14px; display: none; }\n" +
"\n" +
"    /* Main App Layout */\n" +
"    #appView { display: none; height: 100vh; flex-direction: column; }\n" +
"    header.topbar {\n" +
"      background: var(--wine); height: 56px; display: flex; align-items: center; justify-content: space-between;\n" +
"      padding: 0 24px; box-shadow: 0 2px 10px rgba(0,0,0,0.5); z-index: 10;\n" +
"    }\n" +
"    .brand { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: 700; color: var(--accent); }\n" +
"    .user-actions { display: flex; align-items: center; gap: 16px; }\n" +
"    .badge-admin { font-size: 13px; font-weight: 600; color: #fff; background: var(--wine-dark); padding: 6px 12px; border-radius: 20px; }\n" +
"    .btn-logout {\n" +
"      background: var(--wine-dark); border: 1px solid rgba(255,255,255,0.2); color: #fff; padding: 6px 14px;\n" +
"      border-radius: 4px; font-size: 13px; cursor: pointer; transition: 0.2s;\n" +
"    }\n" +
"    .btn-logout:hover { background: var(--error); }\n" +
"\n" +
"    .main-body { display: flex; flex: 1; height: calc(100vh - 56px); }\n" +
"    aside.sidebar {\n" +
"      width: 250px; background: var(--wine-dark); display: flex; flex-direction: column;\n" +
"      padding: 20px 12px; border-right: 1px solid rgba(255,255,255,0.05);\n" +
"    }\n" +
"    .sidebar-heading { font-size: 11px; font-weight: 700; color: var(--accent); padding: 0 12px 12px; letter-spacing: 1px; }\n" +
"    .nav-btn {\n" +
"      background: transparent; border: none; color: var(--text); padding: 12px 16px; border-radius: 6px;\n" +
"      font-size: 14px; font-weight: 600; text-align: left; cursor: pointer; display: flex; align-items: center;\n" +
"      gap: 12px; transition: 0.2s; margin-bottom: 4px;\n" +
"    }\n" +
"    .nav-btn:hover { background: var(--wine-light); color: #fff; }\n" +
"    .nav-btn.active {\n" +
"      background: var(--wine); color: #fff; border-left: 4px solid var(--accent); border-radius: 2px 6px 6px 2px;\n" +
"    }\n" +
"    .sidebar-footer { margin-top: auto; padding: 12px; font-size: 11px; color: var(--text-muted); line-height: 1.5; }\n" +
"\n" +
"    main.content { flex: 1; background: var(--black); padding: 28px 36px; overflow-y: auto; }\n" +
"    .tab-content { display: none; }\n" +
"    .tab-content.active { display: block; }\n" +
"    \n" +
"    h2.page-title { font-size: 24px; font-weight: 700; color: var(--accent); margin-bottom: 4px; }\n" +
"    p.page-desc { font-size: 14px; color: var(--text-muted); margin-bottom: 24px; }\n" +
"\n" +
"    /* Cards & Stats */\n" +
"    .kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 28px; }\n" +
"    .kpi-card {\n" +
"      background: var(--charcoal); padding: 20px; border-radius: 8px; border: 1px solid #2a2a2a;\n" +
"    }\n" +
"    .kpi-title { font-size: 13px; font-weight: 600; color: var(--text-muted); margin-bottom: 8px; }\n" +
"    .kpi-val { font-size: 32px; font-weight: 700; color: var(--text); }\n" +
"    .kpi-card:nth-child(1) .kpi-val { color: var(--accent); }\n" +
"    .kpi-card:nth-child(2) .kpi-val { color: #5dade2; }\n" +
"    .kpi-card:nth-child(3) .kpi-val { color: #f48fb1; }\n" +
"    .kpi-card:nth-child(4) .kpi-val { color: var(--success); }\n" +
"\n" +
"    /* Forms */\n" +
"    .card {\n" +
"      background: var(--charcoal); padding: 26px; border-radius: 8px; border: 1px solid #2a2a2a; margin-bottom: 24px;\n" +
"    }\n" +
"    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 18px 24px; }\n" +
"    .form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px; }\n" +
"    .btn-secondary {\n" +
"      background: #333; color: var(--text); border: none; padding: 10px 20px; border-radius: 6px;\n" +
"      font-size: 14px; font-weight: 600; cursor: pointer; transition: 0.2s;\n" +
"    }\n" +
"    .btn-secondary:hover { background: #444; }\n" +
"    .btn-danger {\n" +
"      background: var(--error); color: #fff; border: none; padding: 10px 20px; border-radius: 6px;\n" +
"      font-size: 14px; font-weight: 600; cursor: pointer; transition: 0.2s;\n" +
"    }\n" +
"    .btn-danger:hover { background: #c0392b; }\n" +
"\n" +
"    /* Tables */\n" +
"    .table-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }\n" +
"    .table-container { background: var(--charcoal); border-radius: 8px; border: 1px solid #2a2a2a; overflow: hidden; }\n" +
"    table.data-table { width: 100%; border-collapse: collapse; text-align: left; font-size: 14px; }\n" +
"    table.data-table th {\n" +
"      background: var(--wine); color: #fff; padding: 14px 16px; font-weight: 700; font-size: 13px; text-transform: uppercase;\n" +
"    }\n" +
"    table.data-table td { padding: 12px 16px; border-bottom: 1px solid #262626; }\n" +
"    table.data-table tr:nth-child(even) { background: var(--charcoal-light); }\n" +
"    table.data-table tr:hover { background: #2f171b; cursor: pointer; }\n" +
"    .badge-dept { background: #332024; color: var(--accent); padding: 4px 8px; border-radius: 4px; font-size: 12px; }\n" +
"\n" +
"    /* Toast Notification */\n" +
"    #toast {\n" +
"      position: fixed; bottom: 24px; right: 24px; background: var(--wine); color: #fff; padding: 14px 22px;\n" +
"      border-radius: 6px; box-shadow: 0 8px 24px rgba(0,0,0,0.6); display: none; z-index: 9999; font-weight: 600;\n" +
"      border-left: 4px solid var(--accent);\n" +
"    }\n" +
"    .mobile-menu-btn {\n" +
"      display: none; background: transparent; border: none; color: var(--accent); font-size: 22px;\n" +
"      cursor: pointer; padding: 4px 8px; margin-right: 6px; line-height: 1;\n" +
"    }\n" +
"    .sidebar-close-btn {\n" +
"      display: none; background: transparent; border: none; color: var(--accent); font-size: 20px;\n" +
"      cursor: pointer; padding: 4px 8px;\n" +
"    }\n" +
"    .sidebar-header-row {\n" +
"      display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;\n" +
"    }\n" +
"    #sidebarBackdrop {\n" +
"      display: none; position: fixed; top: 0; left: 0; right: 0; bottom: 0;\n" +
"      background: rgba(0,0,0,0.75); z-index: 998; backdrop-filter: blur(3px);\n" +
"    }\n" +
"    @media (max-width: 768px) {\n" +
"      .mobile-menu-btn { display: inline-flex; align-items: center; justify-content: center; }\n" +
"      .sidebar-close-btn { display: block; }\n" +
"      header.topbar { padding: 0 14px; height: 52px; }\n" +
"      .brand { font-size: 14px; gap: 6px; }\n" +
"      .badge-admin { display: none; }\n" +
"      .btn-logout { padding: 5px 12px; font-size: 12px; }\n" +
"      aside.sidebar {\n" +
"        position: fixed; left: -290px; top: 0; bottom: 0; width: 270px; height: 100vh;\n" +
"        z-index: 999; box-shadow: 4px 0 30px rgba(0,0,0,0.85);\n" +
"        transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1); padding: 20px 14px;\n" +
"      }\n" +
"      aside.sidebar.mobile-open { left: 0; }\n" +
"      main.content { padding: 18px 14px; height: calc(100vh - 52px); }\n" +
"      h2.page-title { font-size: 20px; }\n" +
"      p.page-desc { font-size: 13px; margin-bottom: 18px; }\n" +
"      .kpi-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 20px; }\n" +
"      .kpi-card { padding: 14px 12px; }\n" +
"      .kpi-title { font-size: 11px; margin-bottom: 6px; }\n" +
"      .kpi-val { font-size: 24px; }\n" +
"      .form-grid { grid-template-columns: 1fr; gap: 14px; }\n" +
"      .form-group[style*=\"span 2\"] { grid-column: span 1 !important; }\n" +
"      .card { padding: 18px 14px; margin-bottom: 18px; }\n" +
"      .table-toolbar { flex-direction: column; align-items: stretch; gap: 10px; }\n" +
"      .table-toolbar input { width: 100% !important; }\n" +
"      .table-toolbar > div { display: flex; justify-content: space-between; }\n" +
"      .table-container { overflow-x: auto; -webkit-overflow-scrolling: touch; }\n" +
"      table.data-table { min-width: 620px; }\n" +
"      table.data-table th, table.data-table td { padding: 10px 12px; font-size: 13px; }\n" +
"      .login-card { width: 90%; max-width: 380px; padding: 28px 18px; }\n" +
"      .logo { font-size: 44px; }\n" +
"      .login-title { font-size: 20px; }\n" +
"      .login-sub { font-size: 13px; margin-bottom: 20px; }\n" +
"      .input-ctrl { font-size: 16px; padding: 11px 14px; }\n" +
"      .btn-primary, .btn-secondary, .btn-danger { padding: 11px 16px; font-size: 13px; }\n" +
"    }\n" +
"  </style>\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"  <!-- 1. LOGIN SCREEN -->\n" +
"  <div id=\"loginView\">\n" +
"    <div class=\"login-card\">\n" +
"      <div class=\"logo\">🎓</div>\n" +
"      <h1 class=\"login-title\">STUDENT PORTAL</h1>\n" +
"      <p class=\"login-sub\">Administrator Sign In</p>\n" +
"      <div id=\"loginAlert\" class=\"alert-banner\"></div>\n" +
"      <form id=\"loginForm\">\n" +
"        <div class=\"form-group\">\n" +
"          <label>Username</label>\n" +
"          <input type=\"text\" id=\"username\" class=\"input-ctrl\" value=\"admin\" required />\n" +
"        </div>\n" +
"        <div class=\"form-group\">\n" +
"          <label>Password</label>\n" +
"          <input type=\"password\" id=\"password\" class=\"input-ctrl\" value=\"admin123\" required />\n" +
"        </div>\n" +
"        <button type=\"submit\" class=\"btn-primary\" style=\"width:100%\">LOGIN TO DASHBOARD</button>\n" +
"      </form>\n" +
"      <div class=\"demo-pill\" onclick=\"fillDemo()\">\n" +
"        ⚡ Click here to auto-fill Demo Account (admin / admin123)\n" +
"      </div>\n" +
"    </div>\n" +
"  </div>\n" +
"\n" +
"  <!-- 2. MAIN APP DASHBOARD -->\n" +
"  <div id=\"appView\">\n" +
"    <div id=\"sidebarBackdrop\" onclick=\"toggleMobileMenu()\"></div>\n" +
"    <header class=\"topbar\">\n" +
"      <div class=\"brand\">\n" +
"        <button class=\"mobile-menu-btn\" onclick=\"toggleMobileMenu()\" aria-label=\"Menu\">☰</button>\n" +
"        <span>🎓</span> STUDENT MANAGEMENT SYSTEM\n" +
"      </div>\n" +
"      <div class=\"user-actions\">\n" +
"        <span class=\"badge-admin\" id=\"adminBadge\">👤 Admin: admin</span>\n" +
"        <button class=\"btn-logout\" onclick=\"logout()\">🚪 Logout</button>\n" +
"      </div>\n" +
"    </header>\n" +
"\n" +
"    <div class=\"main-body\">\n" +
"      <aside class=\"sidebar\" id=\"mainSidebar\">\n" +
"        <div class=\"sidebar-header-row\">\n" +
"          <div class=\"sidebar-heading\" style=\"padding:0;\">MAIN NAVIGATION</div>\n" +
"          <button class=\"sidebar-close-btn\" onclick=\"toggleMobileMenu()\" aria-label=\"Close Menu\">✕</button>\n" +
"        </div>\n" +
"        <button class=\"nav-btn active\" onclick=\"showTab('overview')\">📊  Dashboard Overview</button>\n" +
"        <button class=\"nav-btn\" onclick=\"showTab('add')\">➕  Add Student</button>\n" +
"        <button class=\"nav-btn\" onclick=\"showTab('view')\">📋  View All Students</button>\n" +
"        <button class=\"nav-btn\" onclick=\"showTab('update')\">✏️  Update Student</button>\n" +
"        <button class=\"nav-btn\" onclick=\"showTab('search')\">🔍  Search & Delete</button>\n" +
"        \n" +
"        <div class=\"sidebar-footer\">\n" +
"          <div>Version: 1.0</div>\n" +
"          <div>Theme: Wine Red & Black</div>\n" +
"          <div>Backend: Java & MySQL</div>\n" +
"        </div>\n" +
"      </aside>\n" +
"\n" +
"      <main class=\"content\">\n" +
"        <!-- Tab 1: Overview -->\n" +
"        <div id=\"tab-overview\" class=\"tab-content active\">\n" +
"          <h2 class=\"page-title\">📊 Dashboard Overview</h2>\n" +
"          <p class=\"page-desc\">Summary metrics for institutional enrollment.</p>\n" +
"          <div class=\"kpi-grid\">\n" +
"            <div class=\"kpi-card\">\n" +
"              <div class=\"kpi-title\">TOTAL REGISTERED</div>\n" +
"              <div class=\"kpi-val\" id=\"kpiTotal\">0</div>\n" +
"            </div>\n" +
"            <div class=\"kpi-card\">\n" +
"              <div class=\"kpi-title\">DEPARTMENTS</div>\n" +
"              <div class=\"kpi-val\" id=\"kpiDepts\">0</div>\n" +
"            </div>\n" +
"            <div class=\"kpi-card\">\n" +
"              <div class=\"kpi-title\">MALE STUDENTS</div>\n" +
"              <div class=\"kpi-val\" id=\"kpiMale\">0</div>\n" +
"            </div>\n" +
"            <div class=\"kpi-card\">\n" +
"              <div class=\"kpi-title\">FEMALE STUDENTS</div>\n" +
"              <div class=\"kpi-val\" id=\"kpiFemale\">0</div>\n" +
"            </div>\n" +
"          </div>\n" +
"          <div class=\"card\">\n" +
"            <h3 style=\"color:var(--accent); margin-bottom:12px;\">⚡ Quick Operations</h3>\n" +
"            <div style=\"display:flex; gap:12px; flex-wrap:wrap;\">\n" +
"              <button class=\"btn-primary\" style=\"width:auto;\" onclick=\"showTab('add')\">➕ Register New Student</button>\n" +
"              <button class=\"btn-secondary\" onclick=\"showTab('view')\">📋 Browse All Records</button>\n" +
"              <button class=\"btn-secondary\" onclick=\"showTab('search')\">🔍 Search Student Directory</button>\n" +
"            </div>\n" +
"          </div>\n" +
"        </div>\n" +
"\n" +
"        <!-- Tab 2: Add Student -->\n" +
"        <div id=\"tab-add\" class=\"tab-content\">\n" +
"          <h2 class=\"page-title\">➕ Add New Student</h2>\n" +
"          <p class=\"page-desc\">Register a student into the database with full constraint validation.</p>\n" +
"          <div class=\"card\">\n" +
"            <form id=\"addForm\" onsubmit=\"saveStudent(event)\">\n" +
"              <div class=\"form-grid\">\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Student ID (Numeric)</label>\n" +
"                  <input type=\"number\" id=\"addId\" class=\"input-ctrl\" placeholder=\"e.g. 1011\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Full Name (Letters & Spaces)</label>\n" +
"                  <input type=\"text\" id=\"addName\" class=\"input-ctrl\" placeholder=\"e.g. Rahul Sharma\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Date of Birth</label>\n" +
"                  <input type=\"date\" id=\"addDob\" class=\"input-ctrl\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Gender</label>\n" +
"                  <select id=\"addGender\" class=\"input-ctrl\" required>\n" +
"                    <option value=\"\">-- Select Gender --</option>\n" +
"                    <option value=\"Male\">Male</option>\n" +
"                    <option value=\"Female\">Female</option>\n" +
"                    <option value=\"Other\">Other</option>\n" +
"                  </select>\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Department</label>\n" +
"                  <select id=\"addDept\" class=\"input-ctrl\" required></select>\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Phone Number (10 digits)</label>\n" +
"                  <input type=\"tel\" id=\"addPhone\" class=\"input-ctrl\" placeholder=\"e.g. 9876543210\" maxlength=\"10\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\" style=\"grid-column: span 2;\">\n" +
"                  <label>Email Address</label>\n" +
"                  <input type=\"email\" id=\"addEmail\" class=\"input-ctrl\" placeholder=\"student@example.com\" required />\n" +
"                </div>\n" +
"              </div>\n" +
"              <div class=\"form-actions\">\n" +
"                <button type=\"reset\" class=\"btn-secondary\">Clear Form</button>\n" +
"                <button type=\"submit\" class=\"btn-primary\" style=\"width:auto;\">Save Student Record</button>\n" +
"              </div>\n" +
"            </form>\n" +
"          </div>\n" +
"        </div>\n" +
"\n" +
"        <!-- Tab 3: View All Students -->\n" +
"        <div id=\"tab-view\" class=\"tab-content\">\n" +
"          <h2 class=\"page-title\">📋 Student Directory</h2>\n" +
"          <p class=\"page-desc\">Filter, sort, and export registered students.</p>\n" +
"          <div class=\"table-toolbar\">\n" +
"            <input type=\"text\" id=\"tableSearch\" class=\"input-ctrl\" style=\"width:320px;\" placeholder=\"🔍 Instant search by any field...\" oninput=\"renderStudentsTable()\" />\n" +
"            <div style=\"display:flex; gap:10px;\">\n" +
"              <button class=\"btn-secondary\" onclick=\"loadData()\">↻ Refresh</button>\n" +
"              <button class=\"btn-primary\" style=\"width:auto;\" onclick=\"exportCSV()\">⬇ Export CSV</button>\n" +
"            </div>\n" +
"          </div>\n" +
"          <div class=\"table-container\">\n" +
"            <table class=\"data-table\">\n" +
"              <thead>\n" +
"                <tr>\n" +
"                  <th>ID</th>\n" +
"                  <th>Full Name</th>\n" +
"                  <th>DOB</th>\n" +
"                  <th>Gender</th>\n" +
"                  <th>Department</th>\n" +
"                  <th>Email</th>\n" +
"                  <th>Phone</th>\n" +
"                  <th>Action</th>\n" +
"                </tr>\n" +
"              </thead>\n" +
"              <tbody id=\"studentsTbody\"></tbody>\n" +
"            </table>\n" +
"          </div>\n" +
"        </div>\n" +
"\n" +
"        <!-- Tab 4: Update Student -->\n" +
"        <div id=\"tab-update\" class=\"tab-content\">\n" +
"          <h2 class=\"page-title\">✏️ Update Student Record</h2>\n" +
"          <p class=\"page-desc\">Select a student from the dropdown or table to edit their record.</p>\n" +
"          <div class=\"card\">\n" +
"            <div class=\"form-group\" style=\"margin-bottom:20px;\">\n" +
"              <label>Select Student to Edit</label>\n" +
"              <select id=\"updateSelect\" class=\"input-ctrl\" onchange=\"populateUpdateForm()\"></select>\n" +
"            </div>\n" +
"            <form id=\"updateForm\" onsubmit=\"commitUpdate(event)\">\n" +
"              <div class=\"form-grid\">\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Student ID (Primary Key - Locked)</label>\n" +
"                  <input type=\"number\" id=\"upId\" class=\"input-ctrl\" readonly style=\"background:#1c1c1c;\" />\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Full Name</label>\n" +
"                  <input type=\"text\" id=\"upName\" class=\"input-ctrl\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Date of Birth</label>\n" +
"                  <input type=\"date\" id=\"upDob\" class=\"input-ctrl\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Gender</label>\n" +
"                  <select id=\"upGender\" class=\"input-ctrl\" required>\n" +
"                    <option value=\"Male\">Male</option>\n" +
"                    <option value=\"Female\">Female</option>\n" +
"                    <option value=\"Other\">Other</option>\n" +
"                  </select>\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Department</label>\n" +
"                  <select id=\"upDept\" class=\"input-ctrl\" required></select>\n" +
"                </div>\n" +
"                <div class=\"form-group\">\n" +
"                  <label>Phone Number (10 digits)</label>\n" +
"                  <input type=\"tel\" id=\"upPhone\" class=\"input-ctrl\" maxlength=\"10\" required />\n" +
"                </div>\n" +
"                <div class=\"form-group\" style=\"grid-column: span 2;\">\n" +
"                  <label>Email Address</label>\n" +
"                  <input type=\"email\" id=\"upEmail\" class=\"input-ctrl\" required />\n" +
"                </div>\n" +
"              </div>\n" +
"              <div class=\"form-actions\">\n" +
"                <button type=\"submit\" class=\"btn-primary\" style=\"width:auto;\">Save Changes</button>\n" +
"              </div>\n" +
"            </form>\n" +
"          </div>\n" +
"        </div>\n" +
"\n" +
"        <!-- Tab 5: Search & Delete -->\n" +
"        <div id=\"tab-search\" class=\"tab-content\">\n" +
"          <h2 class=\"page-title\">🔍 Search & Delete Student</h2>\n" +
"          <p class=\"page-desc\">Find a student by ID or Name, inspect details, and remove with confirmation.</p>\n" +
"          <div class=\"card\">\n" +
"            <div style=\"display:flex; gap:12px; margin-bottom:20px; flex-wrap:wrap;\">\n" +
"              <input type=\"text\" id=\"searchQuery\" class=\"input-ctrl\" style=\"flex:1; min-width:200px;\" placeholder=\"Enter Student ID or Name...\" />\n" +
"              <button class=\"btn-primary\" style=\"width:auto;\" onclick=\"doSearch()\">Search</button>\n" +
"              <button class=\"btn-secondary\" onclick=\"resetSearch()\">Reset</button>\n" +
"            </div>\n" +
"            <div id=\"searchResults\" class=\"table-container\" style=\"max-height: 280px; overflow-y:auto;\">\n" +
"              <table class=\"data-table\">\n" +
"                <thead>\n" +
"                  <tr><th>ID</th><th>Name</th><th>Department</th><th>Email</th><th>Action</th></tr>\n" +
"                </thead>\n" +
"                <tbody id=\"searchTbody\"></tbody>\n" +
"              </table>\n" +
"            </div>\n" +
"          </div>\n" +
"        </div>\n" +
"      </main>\n" +
"    </div>\n" +
"  </div>\n" +
"\n" +
"  <!-- Toast Message -->\n" +
"  <div id=\"toast\"></div>\n" +
"\n" +
"  <script>\n" +
"    let allStudents = [];\n" +
"    let allDepartments = [];\n" +
"\n" +
"    function fillDemo() {\n" +
"      document.getElementById('username').value = 'admin';\n" +
"      document.getElementById('password').value = 'admin123';\n" +
"      toast('Demo credentials populated! Click Login.');\n" +
"    }\n" +
"\n" +
"    document.getElementById('loginForm').onsubmit = async (e) => {\n" +
"      e.preventDefault();\n" +
"      const u = document.getElementById('username').value.trim();\n" +
"      const p = document.getElementById('password').value.trim();\n" +
"      const alertBox = document.getElementById('loginAlert');\n" +
"      \n" +
"      try {\n" +
"        const res = await fetch('/api/login', {\n" +
"          method: 'POST',\n" +
"          headers: { 'Content-Type': 'application/json' },\n" +
"          body: JSON.stringify({ username: u, password: p })\n" +
"        });\n" +
"        const data = await res.json();\n" +
"        if (res.ok && data.success) {\n" +
"          document.getElementById('loginView').style.display = 'none';\n" +
"          document.getElementById('appView').style.display = 'flex';\n" +
"          document.getElementById('adminBadge').innerText = '👤 Admin: ' + data.username;\n" +
"          await loadData();\n" +
"          toast('Logged in successfully!');\n" +
"        } else {\n" +
"          alertBox.innerText = data.error || 'Invalid credentials.';\n" +
"          alertBox.style.display = 'block';\n" +
"        }\n" +
"      } catch (err) {\n" +
"        alertBox.innerText = 'Could not reach server: ' + err.message;\n" +
"        alertBox.style.display = 'block';\n" +
"      }\n" +
"    };\n" +
"\n" +
"    function logout() {\n" +
"      if (confirm('Are you sure you want to log out?')) {\n" +
"        document.getElementById('appView').style.display = 'none';\n" +
"        document.getElementById('loginView').style.display = 'flex';\n" +
"        toast('Logged out successfully.');\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function toggleMobileMenu() {\n" +
"      const sb = document.getElementById('mainSidebar');\n" +
"      const bd = document.getElementById('sidebarBackdrop');\n" +
"      const isOpen = sb.classList.toggle('mobile-open');\n" +
"      bd.style.display = isOpen ? 'block' : 'none';\n" +
"    }\n" +
"    function closeMobileMenu() {\n" +
"      const sb = document.getElementById('mainSidebar');\n" +
"      const bd = document.getElementById('sidebarBackdrop');\n" +
"      if (sb) sb.classList.remove('mobile-open');\n" +
"      if (bd) bd.style.display = 'none';\n" +
"    }\n" +
"\n" +
"    function showTab(tabId) {\n" +
"      closeMobileMenu();\n" +
"      document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));\n" +
"      document.querySelectorAll('.nav-btn').forEach(el => el.classList.remove('active'));\n" +
"      \n" +
"      const target = document.getElementById('tab-' + tabId);\n" +
"      if (target) target.classList.add('active');\n" +
"      \n" +
"      const btn = Array.from(document.querySelectorAll('.nav-btn')).find(b => b.innerText.toLowerCase().includes(tabId));\n" +
"      if (btn) btn.classList.add('active');\n" +
"      \n" +
"      if (tabId === 'overview') updateKPIs();\n" +
"      if (tabId === 'view') renderStudentsTable();\n" +
"      if (tabId === 'update') populateUpdateDropdown();\n" +
"      if (tabId === 'search') renderSearchTable(allStudents);\n" +
"    }\n" +
"\n" +
"    async function loadData() {\n" +
"      try {\n" +
"        const [dRes, sRes] = await Promise.all([\n" +
"          fetch('/api/departments'),\n" +
"          fetch('/api/students')\n" +
"        ]);\n" +
"        allDepartments = await dRes.json();\n" +
"        allStudents = await sRes.json();\n" +
"        \n" +
"        populateDeptDropdowns();\n" +
"        updateKPIs();\n" +
"        renderStudentsTable();\n" +
"        populateUpdateDropdown();\n" +
"        renderSearchTable(allStudents);\n" +
"      } catch (e) {\n" +
"        toast('Error loading database: ' + e.message);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function populateDeptDropdowns() {\n" +
"      const addDept = document.getElementById('addDept');\n" +
"      const upDept = document.getElementById('upDept');\n" +
"      let html = '<option value=\"\">-- Select Department --</option>';\n" +
"      allDepartments.forEach(d => {\n" +
"        html += `<option value=\"${d.id}\">${d.name}</option>`;\n" +
"      });\n" +
"      addDept.innerHTML = html;\n" +
"      upDept.innerHTML = html;\n" +
"    }\n" +
"\n" +
"    function updateKPIs() {\n" +
"      document.getElementById('kpiTotal').innerText = allStudents.length;\n" +
"      document.getElementById('kpiDepts').innerText = allDepartments.length;\n" +
"      const males = allStudents.filter(s => s.gender === 'Male').length;\n" +
"      const females = allStudents.filter(s => s.gender === 'Female').length;\n" +
"      document.getElementById('kpiMale').innerText = males;\n" +
"      document.getElementById('kpiFemale').innerText = females;\n" +
"    }\n" +
"\n" +
"    function renderStudentsTable() {\n" +
"      const q = document.getElementById('tableSearch').value.toLowerCase();\n" +
"      const filtered = allStudents.filter(s => \n" +
"        s.id.toString().includes(q) ||\n" +
"        s.name.toLowerCase().includes(q) ||\n" +
"        (s.departmentName && s.departmentName.toLowerCase().includes(q)) ||\n" +
"        s.email.toLowerCase().includes(q)\n" +
"      );\n" +
"      \n" +
"      const tbody = document.getElementById('studentsTbody');\n" +
"      tbody.innerHTML = filtered.map(s => `\n" +
"        <tr>\n" +
"          <td><strong>#${s.id}</strong></td>\n" +
"          <td>${s.name}</td>\n" +
"          <td>${s.dob}</td>\n" +
"          <td>${s.gender}</td>\n" +
"          <td><span class=\"badge-dept\">${s.departmentName || 'N/A'}</span></td>\n" +
"          <td>${s.email}</td>\n" +
"          <td>${s.phone}</td>\n" +
"          <td>\n" +
"            <button class=\"btn-secondary\" style=\"padding:4px 8px; font-size:12px;\" onclick=\"editFromRow(${s.id})\">Edit</button>\n" +
"            <button class=\"btn-danger\" style=\"padding:4px 8px; font-size:12px;\" onclick=\"deleteStudent(${s.id}, '${s.name}')\">Delete</button>\n" +
"          </td>\n" +
"        </tr>\n" +
"      `).join('');\n" +
"    }\n" +
"\n" +
"    async function saveStudent(e) {\n" +
"      e.preventDefault();\n" +
"      const payload = {\n" +
"        id: document.getElementById('addId').value,\n" +
"        name: document.getElementById('addName').value,\n" +
"        dob: document.getElementById('addDob').value,\n" +
"        gender: document.getElementById('addGender').value,\n" +
"        departmentId: document.getElementById('addDept').value,\n" +
"        email: document.getElementById('addEmail').value,\n" +
"        phone: document.getElementById('addPhone').value\n" +
"      };\n" +
"      \n" +
"      const res = await fetch('/api/students', {\n" +
"        method: 'POST',\n" +
"        headers: { 'Content-Type': 'application/json' },\n" +
"        body: JSON.stringify(payload)\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (res.ok) {\n" +
"        toast('✓ Student added successfully!');\n" +
"        document.getElementById('addForm').reset();\n" +
"        await loadData();\n" +
"        showTab('view');\n" +
"      } else {\n" +
"        alert('Validation Error: ' + data.error);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function populateUpdateDropdown() {\n" +
"      const sel = document.getElementById('updateSelect');\n" +
"      sel.innerHTML = '<option value=\"\">-- Choose a student to edit --</option>' +\n" +
"        allStudents.map(s => `<option value=\"${s.id}\">#${s.id} - ${s.name} (${s.departmentName})</option>`).join('');\n" +
"    }\n" +
"\n" +
"    function populateUpdateForm() {\n" +
"      const id = parseInt(document.getElementById('updateSelect').value);\n" +
"      const s = allStudents.find(x => x.id === id);\n" +
"      if (!s) return;\n" +
"      document.getElementById('upId').value = s.id;\n" +
"      document.getElementById('upName').value = s.name;\n" +
"      document.getElementById('upDob').value = s.dob;\n" +
"      document.getElementById('upGender').value = s.gender;\n" +
"      document.getElementById('upDept').value = s.departmentId;\n" +
"      document.getElementById('upEmail').value = s.email;\n" +
"      document.getElementById('upPhone').value = s.phone;\n" +
"    }\n" +
"\n" +
"    function editFromRow(id) {\n" +
"      showTab('update');\n" +
"      document.getElementById('updateSelect').value = id;\n" +
"      populateUpdateForm();\n" +
"    }\n" +
"\n" +
"    async function commitUpdate(e) {\n" +
"      e.preventDefault();\n" +
"      const payload = {\n" +
"        id: document.getElementById('upId').value,\n" +
"        name: document.getElementById('upName').value,\n" +
"        dob: document.getElementById('upDob').value,\n" +
"        gender: document.getElementById('upGender').value,\n" +
"        departmentId: document.getElementById('upDept').value,\n" +
"        email: document.getElementById('upEmail').value,\n" +
"        phone: document.getElementById('upPhone').value\n" +
"      };\n" +
"      \n" +
"      const res = await fetch('/api/students', {\n" +
"        method: 'PUT',\n" +
"        headers: { 'Content-Type': 'application/json' },\n" +
"        body: JSON.stringify(payload)\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (res.ok) {\n" +
"        toast('✓ Record updated successfully!');\n" +
"        await loadData();\n" +
"        showTab('view');\n" +
"      } else {\n" +
"        alert('Error updating: ' + data.error);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function renderSearchTable(list) {\n" +
"      const tbody = document.getElementById('searchTbody');\n" +
"      if (list.length === 0) {\n" +
"        tbody.innerHTML = '<tr><td colspan=\"5\" style=\"text-align:center; color:var(--text-muted);\">No records found.</td></tr>';\n" +
"        return;\n" +
"      }\n" +
"      tbody.innerHTML = list.map(s => `\n" +
"        <tr>\n" +
"          <td><strong>#${s.id}</strong></td>\n" +
"          <td>${s.name}</td>\n" +
"          <td>${s.departmentName || 'N/A'}</td>\n" +
"          <td>${s.email}</td>\n" +
"          <td>\n" +
"            <button class=\"btn-danger\" style=\"padding:4px 10px; font-size:12px;\" onclick=\"deleteStudent(${s.id}, '${s.name}')\">🗑 Delete</button>\n" +
"          </td>\n" +
"        </tr>\n" +
"      `).join('');\n" +
"    }\n" +
"\n" +
"    function doSearch() {\n" +
"      const q = document.getElementById('searchQuery').value.trim().toLowerCase();\n" +
"      const matches = allStudents.filter(s => \n" +
"        s.id.toString().includes(q) ||\n" +
"        s.name.toLowerCase().includes(q) ||\n" +
"        s.email.toLowerCase().includes(q)\n" +
"      );\n" +
"      renderSearchTable(matches);\n" +
"    }\n" +
"\n" +
"    function resetSearch() {\n" +
"      document.getElementById('searchQuery').value = '';\n" +
"      renderSearchTable(allStudents);\n" +
"    }\n" +
"\n" +
"    async function deleteStudent(id, name) {\n" +
"      if (!confirm(`Are you sure you want to permanently delete student #${id} (${name})?\\n\\nThis action cannot be undone.`)) {\n" +
"        return;\n" +
"      }\n" +
"      try {\n" +
"        const res = await fetch('/api/students?id=' + id, { method: 'DELETE' });\n" +
"        if (res.ok) {\n" +
"          toast(`Student #${id} deleted.`);\n" +
"          await loadData();\n" +
"        } else {\n" +
"          alert('Could not delete student.');\n" +
"        }\n" +
"      } catch (e) {\n" +
"        alert('Error: ' + e.message);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function exportCSV() {\n" +
"      let csv = 'Student ID,Full Name,DOB,Gender,Department,Email,Phone\\n';\n" +
"      allStudents.forEach(s => {\n" +
"        csv += `\"${s.id}\",\"${s.name}\",\"${s.dob}\",\"${s.gender}\",\"${s.departmentName}\",\"${s.email}\",\"${s.phone}\"\\n`;\n" +
"      });\n" +
"      const blob = new Blob([csv], { type: 'text/csv' });\n" +
"      const url = window.URL.createObjectURL(blob);\n" +
"      const a = document.createElement('a');\n" +
"      a.href = url;\n" +
"      a.download = 'students_export.csv';\n" +
"      a.click();\n" +
"      toast('Exported students_export.csv');\n" +
"    }\n" +
"\n" +
"    function toast(msg) {\n" +
"      const t = document.getElementById('toast');\n" +
"      t.innerText = msg;\n" +
"      t.style.display = 'block';\n" +
"      setTimeout(() => { t.style.display = 'none'; }, 3000);\n" +
"    }\n" +
"  </script>\n" +
"</body>\n" +
"</html>";
    }
}

