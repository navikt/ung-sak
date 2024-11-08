package no.nav.k9.sak.mottak;

import java.util.Comparator;

import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;

class MottattDokumentSorterer {
    private static Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareToIgnoreCase);

    private MottattDokumentSorterer() {
        // skjul public constructor
    }

    static Comparator<MottattDokument> sorterMottattDokument() {
        return Comparator.comparing(MottattDokument::getMottattDato)
            .thenComparing(MottattDokument::getKanalreferanse, nullSafeStringComparator);
    }
}
