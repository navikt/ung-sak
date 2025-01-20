package no.nav.ung.sak.mottak;

import java.util.Comparator;

import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;

class MottattDokumentSorterer {

    private MottattDokumentSorterer() {
        // skjul public constructor
    }

    static Comparator<MottattDokument> sorterMottattDokument() {
        return Comparator.comparing(MottattDokument::getMottattTidspunkt);
    }
}
