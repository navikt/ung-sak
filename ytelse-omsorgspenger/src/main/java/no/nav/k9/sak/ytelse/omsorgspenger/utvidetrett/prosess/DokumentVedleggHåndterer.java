package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.LogiskVedleggResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.sak.typer.JournalpostId;

@Dependent
public class DokumentVedleggHåndterer {

    private SafTjeneste safTjeneste;

    @Inject
    public DokumentVedleggHåndterer(SafTjeneste safTjeneste) {
        this.safTjeneste = safTjeneste;
    }

    public boolean harVedlegg(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var projection = new JournalpostResponseProjection()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection()
                    .variantformat()
                    .filnavn()
                    .filtype()
                    .saksbehandlerHarTilgang())
                .logiskeVedlegg(new LogiskVedleggResponseProjection()
                    .tittel()))
            .datoOpprettet()
            .relevanteDatoer(new RelevantDatoResponseProjection()
                .dato()
                .datotype());
        var journalpost = safTjeneste.hentJournalpostInfo(query, projection);

        return journalpost.getDokumenter().size() > 1; // mer enn hoveddokument

    }

}
