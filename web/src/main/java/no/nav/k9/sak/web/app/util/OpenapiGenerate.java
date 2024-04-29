package no.nav.k9.sak.web.app.util;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import no.nav.k9.sak.web.app.ApplicationConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenapiGenerate {
    public static void main(String[] args) throws IOException {
        final OpenAPI resolved = ApplicationConfig.resolveOpenAPI();

        final var outputPath = args.length > 0 ? args[0] : "";
        if(!outputPath.isEmpty()) {
            if(outputPath.endsWith(".json")) {
                // Ok, write the generated openapi spec to given file path
                final Path path = Paths.get(outputPath);
                final String outputJson = Json.pretty(resolved);
                Files.writeString(path, outputJson);
            } else {
                throw new RuntimeException("OpenapiGenerate called with invalid outputPath argument ("+ outputPath + ")");
            }
        } else {
            // No outputPath provided, print generated json to stdout
            Json.prettyPrint(resolved);
        }
    }
}
