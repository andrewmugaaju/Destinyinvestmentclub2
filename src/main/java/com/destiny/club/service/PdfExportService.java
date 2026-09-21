package com.destiny.club.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Renders a Thymeleaf template to a PDF byte array, for the "export to PDF" buttons on the
 * accounting reports. The Thymeleaf output (regular HTML5, not strict XML) is parsed leniently
 * with jsoup and converted to a W3C DOM document, since the PDF renderer requires well-formed
 * XHTML input.
 */
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final SpringTemplateEngine templateEngine;

    public byte[] renderPdf(String templateName, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        String html = templateEngine.process(templateName, context);

        Document jsoupDocument = Jsoup.parse(html);
        jsoupDocument.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        org.w3c.dom.Document w3cDocument = new W3CDom().fromJsoup(jsoupDocument);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withW3cDocument(w3cDocument, "");
        builder.toStream(outputStream);
        try {
            builder.run();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render PDF for template " + templateName, e);
        }
        return outputStream.toByteArray();
    }
}
