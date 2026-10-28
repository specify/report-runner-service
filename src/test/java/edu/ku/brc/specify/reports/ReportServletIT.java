package edu.ku.brc.specify.reports;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;

import static org.junit.Assert.*;

/** Exercises the packaged application on the same Jetty EE8 runtime as Docker. */
public class ReportServletIT {
    private static Server server;
    private static URI reportUri;

    private static final String REPORT = """
        <?xml version="1.0" encoding="UTF-8"?>
        <jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports
                http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
            name="runtime-smoke" pageWidth="300" pageHeight="200" columnWidth="260"
            leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20">
            <parameter name="heading" class="java.lang.String"/>
            <field name="name" class="java.lang.String"/>
            <detail><band height="40"><textField>
                <reportElement x="0" y="0" width="260" height="30"/>
                <textFieldExpression><![CDATA[$P{heading} + ": " + $F{name}]]></textFieldExpression>
            </textField></band></detail>
        </jasperReport>
        """;

    @BeforeClass
    public static void startApplication() throws Exception {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(0);
        server.addConnector(connector);

        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(Path.of("target/minimal_reports-1.0-SNAPSHOT").toAbsolutePath().toString());
        webapp.setThrowUnavailableOnStartupException(true);
        server.setHandler(webapp);
        server.start();
        reportUri = URI.create("http://127.0.0.1:" + connector.getLocalPort() + "/report");
    }

    @AfterClass
    public static void stopApplication() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void rendersExistingJrxmlWithDataAndParameters() throws Exception {
        assertPdf(postReport(REPORT));
    }

    @Test
    public void acceptsFormsLargerThanJettysDefaultLimit() throws Exception {
        // The old deployment accepted 100 MB forms. Jetty's default is 200 KB.
        assertPdf(postReport(REPORT.replace("</jasperReport>",
            "<!--" + "x".repeat(300_000) + "--></jasperReport>")));
    }

    @Test
    public void rendersReadableQrCode() throws Exception {
        String qrCode = """
            <componentElement>
                <reportElement x="0" y="35" width="80" height="80"/>
                <c:QRCode xmlns:c="http://jasperreports.sourceforge.net/jasperreports/components"
                    xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports/components
                        http://jasperreports.sourceforge.net/xsd/components.xsd">
                    <c:codeExpression><![CDATA[$F{name}]]></c:codeExpression>
                </c:QRCode>
            </componentElement>
            """;
        String report = REPORT.replace("band height=\"40\"", "band height=\"120\"")
            .replace("</band>", qrCode + "</band>");
        HttpResponse<byte[]> response = postReport(report);
        assertPdf(response);
        try (PDDocument pdf = Loader.loadPDF(response.body())) {
            var image = new PDFRenderer(pdf).renderImageWithDPI(0, 200);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image)));
            assertEquals("Specimen 123", new MultiFormatReader().decode(bitmap).getText());
        }
    }

    @Test
    public void rendersSvgUsingAlignedBatikDependencies() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"80\" height=\"80\">"
            + "<rect width=\"80\" height=\"80\" fill=\"blue\"/></svg>";
        String image = """
            <image scaleImage="FillFrame">
                <reportElement x="0" y="35" width="80" height="80"/>
                <imageExpression><![CDATA[new java.io.ByteArrayInputStream(
                    java.util.Base64.getDecoder().decode("%s"))]]></imageExpression>
            </image>
            """.formatted(Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8)));
        String report = REPORT.replace("band height=\"40\"", "band height=\"120\"")
            .replace("</band>", image + "</band>");
        HttpResponse<byte[]> response = postReport(report);
        assertPdf(response);
        try (PDDocument pdf = Loader.loadPDF(response.body())) {
            var rendered = new PDFRenderer(pdf).renderImageWithDPI(0, 72);
            assertEquals(0x0000ff, rendered.getRGB(40, 75) & 0xffffff);
        }
    }

    private HttpResponse<byte[]> postReport(String report) throws Exception {
        String body = "report=" + encode(report)
            + "&data=" + encode("{\"fields\":[\"name\"],\"rows\":[[\"Specimen 123\"]]}")
            + "&parameters=" + encode("{\"heading\":\"Collection label\"}");
        HttpRequest request = HttpRequest.newBuilder(reportUri)
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void assertPdf(HttpResponse<byte[]> response) throws Exception {
        assertEquals(new String(response.body(), StandardCharsets.UTF_8), 200, response.statusCode());
        assertEquals("application/pdf", response.headers().firstValue("Content-Type").orElse(""));
        try (PDDocument pdf = Loader.loadPDF(response.body())) {
            assertEquals(1, pdf.getNumberOfPages());
            assertTrue(new PDFTextStripper().getText(pdf).contains("Collection label: Specimen 123"));
        }
    }
}
