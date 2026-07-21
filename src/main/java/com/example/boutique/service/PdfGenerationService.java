package com.example.boutique.service;

import com.example.boutique.utils.OkHttpStreamFactory;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.w3c.dom.Document;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

@Service
public class PdfGenerationService {

    private final TemplateEngine templateEngine;
    private final OkHttpStreamFactory okHttpStreamFactory;

    @Autowired
    public PdfGenerationService(TemplateEngine templateEngine, OkHttpStreamFactory okHttpStreamFactory) {
        this.templateEngine = templateEngine;
        this.okHttpStreamFactory = okHttpStreamFactory;
        XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));
    }

    public byte[] generatePdfFromHtml(String templateName, Map<String, Object> data) throws IOException {
        Context context = new Context();
        context.setVariables(data);
        String html = templateEngine.process(templateName, context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();

        // Utiliser Jsoup pour parser le HTML
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
        jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml); // Use XML syntax

        Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);


        builder.useHttpStreamImplementation(okHttpStreamFactory); // Use the injected factory
        builder.withW3cDocument(w3cDoc, "classpath:/templates/");
        builder.toStream(outputStream);
        builder.run();

        return outputStream.toByteArray();
    }
}
