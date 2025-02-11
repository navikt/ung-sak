package no.nav.ung.sak.web.app.util;

import io.swagger.v3.oas.models.OpenAPI;
import no.nav.openapi.spec.utils.openapi.FileOutputter;
import no.nav.ung.sak.web.app.ApplicationConfig;

import java.io.IOException;

public class OpenapiGenerate {

    public static void main(String[] args) throws IOException {
        final ApplicationConfig applicationConfig = new ApplicationConfig();
        final OpenAPI resolved = applicationConfig.resolveOpenAPI();
        final var outputPath = args.length > 0 ? args[0] : "";
        FileOutputter.writeJsonFile(resolved, outputPath);
    }
}
