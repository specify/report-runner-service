package edu.ku.brc.specify.reports;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.sf.jasperreports.engine.*;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: ben
 * Date: 8/14/14
 * Time: 5:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String reportStr = request.getParameter("report");
        String data = request.getParameter("data");
        String parameters = request.getParameter("parameters");
        ByteArrayInputStream reportIS = new ByteArrayInputStream(reportStr.getBytes(StandardCharsets.UTF_8));
        JRDataSource dataSource = new DataSource(data);
        JsonParser parser = new JsonParser();
        Map<String, Object> params = new HashMap<>();
        JsonObject parametersParsed = parser.parse(parameters).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : parametersParsed.entrySet()) {
            params.put(entry.getKey(), entry.getValue().getAsString());
        }

        byte[] pdf;
        try {
            JasperReport report = JasperCompileManager.compileReport(reportIS);
            JasperPrint filledReport = JasperFillManager.fillReport(report, params, dataSource);
            pdf = JasperExportManager.exportReportToPdf(filledReport);
        } catch (JRException e) {
            throw new ServletException(e);
        }

        response.setContentType("application/pdf");
        response.setContentLength(pdf.length);

        ServletOutputStream servletOutputStream = response.getOutputStream();
        servletOutputStream.write(pdf, 0, pdf.length);
        servletOutputStream.flush();
        servletOutputStream.close();
    }
}