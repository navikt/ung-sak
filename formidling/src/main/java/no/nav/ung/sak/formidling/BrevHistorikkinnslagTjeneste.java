package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.*;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.BrevbestillingEntitet;
import no.nav.ung.sak.typer.JournalpostId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Dependent
public class BrevHistorikkinnslagTjeneste {

    Logger log = LoggerFactory.getLogger(BrevHistorikkinnslagTjeneste.class);

    private final HistorikkinnslagRepository historikkRepository;
    private final Saf safTjeneste;


    @Inject
    public BrevHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkRepository, Saf safTjeneste) {
        this.historikkRepository = historikkRepository;
        this.safTjeneste = safTjeneste;
    }

    public void opprett(BrevbestillingEntitet bestilling) {

        var linje = bestilling.getTemplateType().getBeskrivelse();
        var builder = new Historikkinnslag.Builder()
            .medFagsakId(bestilling.getFagsakId())
            .medBehandlingId(bestilling.getBehandlingId())
            .medAktør(bestilling.isVedtaksbrev() ? HistorikkAktør.VEDTAKSLØSNINGEN : HistorikkAktør.SAKSBEHANDLER)
            .medTittel(bestilling.isVedtaksbrev() ? "Vedtaksbrev bestilt" : "Brev bestilt");

        var journalpostId = bestilling.getJournalpostId();
        Journalpost journalpost = hentJournalpost(journalpostId);

        if (journalpost != null && !journalpost.getDokumenter().isEmpty()) {
            var hoveddokumentJournalMetadata = journalpost.getDokumenter().getFirst();
            builder.medDokumenter(List.of(byggDokumentLink(hoveddokumentJournalMetadata, journalpost)));
            if (journalpost.getKanalnavn() != null) {
                linje += " (" + journalpost.getKanalnavn() + ")";
            }
        } else {
            log.warn("Ingen dokumenter funnet for journalpostId: {} ved generering av historikkinnslag. Dropper å nevne dokumentet", journalpostId);
        }

        builder.addLinje(linje);

        historikkRepository.lagre(builder.build());
    }

    private static HistorikkinnslagDokumentLink byggDokumentLink(DokumentInfo hoveddokumentJournalMetadata, Journalpost journalpost) {
        return new HistorikkinnslagDokumentLink.Builder()
            .medDokumentId(hoveddokumentJournalMetadata.getDokumentInfoId())
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medLinkTekst("Brev")
            .build();
    }

    private Journalpost hentJournalpost(String journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId);

        try {
           return safTjeneste.hentJournalpostInfo(query, new JournalpostResponseProjection()
                .journalpostId()
                .tittel()
                .journalposttype()
                .journalstatus()
                .kanal()
                .kanalnavn()
                .dokumenter(new DokumentInfoResponseProjection()
                    .dokumentInfoId()));
        } catch (Exception e) {
            log.error("Feil ved henting av journalpost med id: {}", journalpostId, e);
            return null;
        }

    }

}
