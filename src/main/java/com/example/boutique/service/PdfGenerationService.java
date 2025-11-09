package com.example.boutique.service;

import com.example.boutique.utils.OkHttpStreamFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;
    private final OkHttpStreamFactory okHttpStreamFactory;

    @Autowired
    public PdfGenerationService(TemplateEngine templateEngine, OkHttpStreamFactory okHttpStreamFactory) {
        this.templateEngine = templateEngine;
        this.okHttpStreamFactory = okHttpStreamFactory;
    }

    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) throws IOException {
        Context context = new Context();
        context.setVariables(data);
        String html = templateEngine.process(templateName, context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useHttpStreamImplementation(okHttpStreamFactory); // Use the injected factory
        builder.withHtmlContent(html, "classpath:/templates/"); // Set a base URI
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }
}
