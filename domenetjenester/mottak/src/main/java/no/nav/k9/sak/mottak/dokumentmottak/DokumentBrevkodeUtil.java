package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.Collection;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.mottak.repo.MottattDokument;

public class DokumentBrevkodeUtil {

    static Brevkode unikBrevkode(Collection<MottattDokument> mottattDokument) {
        if (mottattDokument == null || mottattDokument.isEmpty()) {
            throw new IllegalArgumentException("MottattDokument er null eller empty");
        }
        var typer = mottattDokument.stream().map(MottattDokument::getType).distinct().collect(Collectors.toList());
        if (typer.size() > 1) {
            throw new UnsupportedOperationException("Støtter ikke mottatt dokument med ulike typer: " + typer);
        } else {
            return typer.get(0);
        }
    }
}
