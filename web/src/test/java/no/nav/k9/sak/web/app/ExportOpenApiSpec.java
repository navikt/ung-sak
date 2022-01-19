package no.nav.k9.sak.web.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ExportOpenApiSpec {

    public static void main(String[] args) throws Exception {
        String spec = new ApplicationConfig().getOpenApiSpec();
        var path = Path.of("openapi.yaml");
        Files.writeString(path, spec, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

    }
}
