package com.example.boutique.utils;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component; // Import for @Component

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Component // Mark this class as a Spring component
public class OkHttpStreamFactory implements FSStreamFactory {

    private final OkHttpClient client;

    public OkHttpStreamFactory() {
        this.client = new OkHttpClient();
    }

    @Override
    public FSStream getUrl(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            final Response response = client.newCall(request).execute();

            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected code " + response);
            }

            return new FSStream() {
                @Override
                public InputStream getStream() {
                    return response.body().byteStream();
                }

                @Override
                public Reader getReader() {
                    return new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8);
                }
            };
        } catch (IOException e) {
            // Wrap the IOException in a RuntimeException to propagate the error
            throw new RuntimeException("Failed to fetch URL: " + url, e);
        }
    }
}
