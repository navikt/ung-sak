package no.nav.ung.domenetjenester.arkiv;

import no.nav.k9.felles.integrasjon.saf.BrukerResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SakResponseProjection;

public class JournalpostProjectionBuilder {

    private static final JournalpostResponseProjection JOURNALPOST_RESPONSE_PROJECTION = new JournalpostResponseProjection()
            .journalpostId()
            .tittel()
            .journalposttype()
            .journalstatus()
            .datoOpprettet()
            .relevanteDatoer(new RelevantDatoResponseProjection()
                    .dato()
                    .datotype()
            )
            .kanal()
            .tema()
            .behandlingstema()
            .sak(new SakResponseProjection()
                    .arkivsaksnummer()
                    .arkivsaksystem()
                    .fagsaksystem()
                    .fagsakId()
            )
            .bruker(new BrukerResponseProjection()
                    .id()
                    .type()
            )
            .journalforendeEnhet()
            .dokumenter(new DokumentInfoResponseProjection()
                    .dokumentInfoId()
                    .tittel()
                    .brevkode()
                    .dokumentvarianter(new DokumentvariantResponseProjection()
                            .variantformat()
                            .filnavn()
                    ));

    /**
     * Spesifiser feltene som skal returneres fra {@link JournalpostQueryRequest}
     */
    public static JournalpostResponseProjection byggJournalpostResponseProjection() {
        return JOURNALPOST_RESPONSE_PROJECTION;
    }

}
