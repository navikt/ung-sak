package no.nav.ung.sak.formidling.template;

/**
 * Pdfgen mal filer
 */
public enum TemplateType {
    INNVILGELSE("innvilgelse");

    final String path;
    final String dir = "ungdomsytelse";

    TemplateType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getDir() {
        return dir;
    }
}
