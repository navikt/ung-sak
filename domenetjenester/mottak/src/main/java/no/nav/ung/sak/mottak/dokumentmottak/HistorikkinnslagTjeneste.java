package no.nav.ung.sak.mottak.dokumentmottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.*;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.typer.JournalpostId;

import java.util.ArrayList;
import java.util.List;

@Dependent
public class HistorikkinnslagTjeneste {

    private static final String VEDLEGG = "Vedlegg";
    private static final String SØKNAD = "Søknad";
    private static final String INNSENDING = "Innsending";
    private static final String INNTEKTSRAPPORTERING = "Inntektsrapportering";
    private static final String OPPGAVEBEKREFTELSE = "Svar på varsel";
    private HistorikkinnslagRepository historikkinnslagRepository;
    private EtterlysningRepository etterlysningRepository;
    private SafTjeneste safTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository,
                                    EtterlysningRepository etterlysningRepository,
                                    SafTjeneste safTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.etterlysningRepository = etterlysningRepository;
        this.safTjeneste = safTjeneste;
    }




    private void leggTilHistorikkinnslagDokumentlinker(JournalpostId journalpostId, Long behandlingId, Historikkinnslag.Builder historikkinnslagBuilder) {
        List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            var query = new JournalpostQueryRequest();
            query.setJournalpostId(journalpostId.getVerdi());
            JournalpostResponseProjection projection = byggDokumentoversiktResponseProjection();
            Journalpost journalpostIdData = safTjeneste.hentJournalpostInfo(query, projection);
            if (journalpostIdData == null || journalpostIdData.getDokumenter().isEmpty()) {
                return;
            }
            DokumentInfo hoveddokumentJournalMetadata = journalpostIdData.getDokumenter().get(0);

            String linkTekstHoveddokument = finnLinkTekstHoveddokument(hoveddokumentJournalMetadata, journalpostId, behandlingId);
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(hoveddokumentJournalMetadata, journalpostId, linkTekstHoveddokument));

            getVedleggsliste(journalpostIdData).forEach(vedleggJournalMetadata ->
                dokumentLinker.add(lagHistorikkInnslagDokumentLink(vedleggJournalMetadata, journalpostId, VEDLEGG)));
        }

        historikkinnslagBuilder.medDokumenter(dokumentLinker);
    }

    private List<DokumentInfo> getVedleggsliste(Journalpost journalpostIdData) {
        var dokumenter = journalpostIdData.getDokumenter();
        if (dokumenter.size() > 1) {
            return dokumenter.subList(1, dokumenter.size());
        }
        return List.of();
    }

    private String finnLinkTekstHoveddokument(DokumentInfo hoveddokumentJournalMetadata, JournalpostId journalpostId, Long behandlingId) {
        Brevkode brevkode = Brevkode.fraKode(hoveddokumentJournalMetadata.getBrevkode());
        if (brevkode == null) {
            return INNSENDING;
        }
        if (brevkode.equals(Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING)) {
            return INNTEKTSRAPPORTERING;
        }
        if (brevkode.equals(Brevkode.UNGDOMSYTELSE_VARSEL_UTTALELSE)) {
            return etterlysningRepository.hentEtterlysninger(behandlingId).stream()
                .filter(it -> it.getUttalelse().stream()
                .anyMatch(u -> u.getSvarJournalpostId().equals(journalpostId)))
                .findFirst()
                .map(Etterlysning::getType)
                .map(EtterlysningType::getNavn)
                .orElse(OPPGAVEBEKREFTELSE);
        }
        if (Brevkode.SØKNAD_TYPER.contains(brevkode)) {
            return SØKNAD;
        }
        return INNSENDING;
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(DokumentInfo journalMetadata, JournalpostId journalpostId, String linkTekst) {
        HistorikkinnslagDokumentLink historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(journalMetadata.getDokumentInfoId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
        return historikkinnslagDokumentLink;
    }

    public void opprettHistorikkinnslagForVedlegg(Long fagsakId, Long behandlingId, JournalpostId journalpostId) {
        var historikkinnslagBuilder = new Historikkinnslag.Builder();
        historikkinnslagBuilder.medAktør(HistorikkAktør.SØKER);
        historikkinnslagBuilder.medTittel("Vedlegg mottatt");
        historikkinnslagBuilder.medFagsakId(fagsakId);



        leggTilHistorikkinnslagDokumentlinker(journalpostId, behandlingId, historikkinnslagBuilder);

        historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
    }

    private JournalpostResponseProjection byggDokumentoversiktResponseProjection() {
        return new JournalpostResponseProjection()
            .journalpostId()
            .tittel()
            .journalposttype()
            .journalstatus()
            .kanal()
            .tema()
            .behandlingstema()
            .sak(new SakResponseProjection()
                .fagsaksystem()
                .fagsakId())
            .bruker(new BrukerResponseProjection()
                .id()
                .type())
            .avsenderMottaker(new AvsenderMottakerResponseProjection()
                .id()
                .type()
                .navn())
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection()
                    .variantformat()
                    .filnavn()
                    .filtype()
                    .saksbehandlerHarTilgang()
                ))
            .relevanteDatoer(new RelevantDatoResponseProjection()
                .dato()
                .datotype()
            );
    }
}
