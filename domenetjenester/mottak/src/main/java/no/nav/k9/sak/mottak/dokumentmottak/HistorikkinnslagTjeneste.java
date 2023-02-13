package no.nav.k9.sak.mottak.dokumentmottak;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.felles.integrasjon.saf.AvsenderMottakerResponseProjection;
import no.nav.k9.felles.integrasjon.saf.BrukerResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentInfo;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.DokumentvariantResponseProjection;
import no.nav.k9.felles.integrasjon.saf.Journalpost;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.RelevantDatoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SakResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;

@Dependent
public class HistorikkinnslagTjeneste {

    public static final String INNTEKTSMELDING_BREVKODE = Brevkode.INNTEKTSMELDING.getOffisiellKode();
    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String INNSENDING = "Innsending";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    private HistorikkRepository historikkRepository;
    private SafTjeneste safTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository,
                                    SafTjeneste safTjeneste) {
        this.historikkRepository = historikkRepository;
        this.safTjeneste = safTjeneste;
    }

    public void opprettHistorikkinnslag(Behandling behandling, JournalpostId journalpostId, HistorikkinnslagType historikkinnslagType) {
        if (historikkinnslagForBehandlingStartetErLoggetTidligere(behandling.getId(), historikkinnslagType)) {
            return;
        }

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        leggTilHistorikkinnslagDokumentlinker(journalpostId, historikkinnslag);

        new HistorikkInnslagTekstBuilder().medHendelse(historikkinnslagType).build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    private boolean historikkinnslagForBehandlingStartetErLoggetTidligere(Long behandlingId, HistorikkinnslagType historikkinnslagType) {
        List<Historikkinnslag> eksisterendeHistorikkListe = historikkRepository.hentHistorikk(behandlingId);

        if (!eksisterendeHistorikkListe.isEmpty()) {
            for (Historikkinnslag eksisterendeHistorikk : eksisterendeHistorikkListe) {
                if (historikkinnslagType.equals(eksisterendeHistorikk.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void leggTilHistorikkinnslagDokumentlinker(JournalpostId journalpostId, Historikkinnslag historikkinnslag) {
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

            String linkTekstHoveddokument = finnLinkTekstHoveddokument(hoveddokumentJournalMetadata);
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(hoveddokumentJournalMetadata, journalpostId, historikkinnslag, linkTekstHoveddokument));

            getVedleggsliste(journalpostIdData).forEach(vedleggJournalMetadata ->
                dokumentLinker.add(lagHistorikkInnslagDokumentLink(vedleggJournalMetadata, journalpostId, historikkinnslag, VEDLEGG)));
        }

        historikkinnslag.setDokumentLinker(dokumentLinker);
    }

    private List<DokumentInfo> getVedleggsliste(Journalpost journalpostIdData) {
        var dokumenter = journalpostIdData.getDokumenter();
        if (dokumenter.size() > 1) {
            return dokumenter.subList(1, dokumenter.size());
        }
        return List.of();
    }

    private String finnLinkTekstHoveddokument(DokumentInfo hoveddokumentJournalMetadata) {
        return hoveddokumentJournalMetadata.getBrevkode().equals(INNTEKTSMELDING_BREVKODE) ? INNTEKTSMELDING : INNSENDING;
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(DokumentInfo journalMetadata, JournalpostId journalpostId, Historikkinnslag historikkinnslag, String linkTekst) {
        HistorikkinnslagDokumentLink historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(journalMetadata.getDokumentInfoId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
        historikkinnslagDokumentLink.setHistorikkinnslag(historikkinnslag);
        return historikkinnslagDokumentLink;
    }

    public void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId, Brevkode dokumentTypeId) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        if (dokumentTypeId != null && dokumentTypeId.equals(Brevkode.INNTEKTSMELDING)) {
            historikkinnslag.setAktør(HistorikkAktør.ARBEIDSGIVER);
        } else {
            historikkinnslag.setAktør(HistorikkAktør.SØKER);
        }
        historikkinnslag.setType(HistorikkinnslagType.VEDLEGG_MOTTATT);
        historikkinnslag.setFagsakId(fagsakId);

        leggTilHistorikkinnslagDokumentlinker(journalpostId, historikkinnslag);

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDLEGG_MOTTATT);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(behandlingÅrsakType);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
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
