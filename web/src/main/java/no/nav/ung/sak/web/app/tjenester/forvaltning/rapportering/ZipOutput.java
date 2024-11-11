package no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.ws.rs.core.StreamingOutput;

import no.nav.ung.sak.web.app.tjenester.forvaltning.DumpOutput;

class ZipOutput {

    private void addToZip(ZipOutputStream zipOut, DumpOutput dump) {
        var zipEntry = new ZipEntry(dump.getPath());
        try {
            zipOut.putNextEntry(zipEntry);
            zipOut.write(dump.getContent().getBytes(Charset.forName("UTF8")));
            zipOut.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Kunne ikke zippe dump fra : " + dump, e);
        }
    }

    StreamingOutput dump(List<DumpOutput> outputs) {
        StreamingOutput streamingOutput = outputStream -> {
            try (ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));) {
                outputs.forEach(dump -> addToZip(zipOut, dump));
            } finally {
                outputStream.flush();
                outputStream.close();
            }
        };
        return streamingOutput;
    }
}
