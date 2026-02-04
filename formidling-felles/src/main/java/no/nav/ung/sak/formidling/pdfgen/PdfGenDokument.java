package no.nav.ung.sak.formidling.pdfgen;

public record PdfGenDokument(
    byte[] pdf,
    String html
) {
}
