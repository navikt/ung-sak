package no.nav.k9.sak.web.app.tjenester.forvaltning.dump;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

public final class DumpMottaker {

    private final Fagsak fagsak;
    private final ZipOutputStream outputStream;

    DumpMottaker(Fagsak fagsak, ZipOutputStream outputStream) {
        this.fagsak = fagsak;
        this.outputStream = outputStream;
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    /**
     * Uncloseable fordi lukking skal håndteres til slutt i {@link DebugDumpsters}
     */
    public OutputStream getOutputStream() {
        return new UncloseableOutputStreamWrapper(outputStream);
    }

    public void newFile(String path) {
        var zipEntry = new ZipEntry(fagsak.getSaksnummer().getVerdi() + "/" + path);
        try {
            outputStream.closeEntry();
            outputStream.putNextEntry(zipEntry);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeErrorFile(String path, Exception e) {
        newFile(path + "-ERROR.txt");
        PrintWriter pw = new PrintWriter(outputStream);
        e.printStackTrace(pw);
        pw.flush();
    }

    private static class UncloseableOutputStreamWrapper extends FilterOutputStream {

        private UncloseableOutputStreamWrapper(OutputStream out) {
            super(out);
        }

        @Override
        public void close() {
        }
    }
}
