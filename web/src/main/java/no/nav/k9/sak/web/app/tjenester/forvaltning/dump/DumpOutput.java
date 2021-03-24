package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.util.Objects;

public class DumpOutput {

    private final String relativePath;
    private final String content;

    public DumpOutput(String relativePath, String content) {
        this.relativePath = Objects.requireNonNull(relativePath);
        this.content = Objects.requireNonNull(content);
    }

    public String getPath() {
        return relativePath;
    }

    public String getContent() {
        return content;
    }

}
