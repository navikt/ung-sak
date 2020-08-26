package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.saf.AvsenderMottakerResponseProjection;
import no.nav.saf.BrukerResponseProjection;
import no.nav.saf.DokumentInfo;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentvariantResponseProjection;
import no.nav.saf.Journalpost;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.RelevantDatoResponseProjection;
import no.nav.saf.SakResponseProjection;
import no.nav.saf.Variantformat;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;

@Dependent
public class HistorikkinnslagTjeneste {

    public static final String INNTEKTSMELDING_BREVKODE = Brevkode.INNTEKTSMELDING.getOffisiellKode();
    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
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

        leggTilHistorikkinnslagDokumentlinker(behandling.getType(), journalpostId, historikkinnslag);

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

    void leggTilHistorikkinnslagDokumentlinker(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag) {
        List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            var query = new JournalpostQueryRequest();
            query.setJournalpostId(journalpostId.getVerdi());
            JournalpostResponseProjection projection = byggDokumentoversiktResponseProjection();
            var journalpostIdData = safTjeneste.hentJournalpostInfo(query, projection);
            if (journalpostIdData == null || journalpostIdData.getDokumenter().isEmpty()) {
                return;
            }
            var hoveddokumentJournalMetadata = journalpostIdData.getDokumenter().get(0);

            var elektroniskSøknad = Optional.ofNullable(hoveddokumentJournalMetadata)
                .stream()
                .filter(it -> it.getDokumentvarianter()
                    .stream()
                    .anyMatch(ta -> Objects.equals(Variantformat.ORIGINAL, ta.getVariantformat()))) // Ustrukturerte dokumenter kan ha xml med variantformat SKANNING_META
                .findFirst();

            leggTilSøknadDokumentLenke(behandlingType, journalpostId, historikkinnslag, dokumentLinker, hoveddokumentJournalMetadata, elektroniskSøknad);
            getVedleggsliste(journalpostIdData).forEach(journalMetadata -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, VEDLEGG)));
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

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag, List<HistorikkinnslagDokumentLink> dokumentLinker,
                                            DokumentInfo hoveddokumentJournalMetadata, Optional<DokumentInfo> elektroniskSøknad) {
        if (elektroniskSøknad.isPresent()) {
            var journalMetadata = elektroniskSøknad.get();
            String linkTekst = journalMetadata.getBrevkode().equals(INNTEKTSMELDING_BREVKODE) ? INNTEKTSMELDING : SØKNAD; // NOSONAR
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, linkTekst));
        } else {
            String linkTekst = BehandlingType.UDEFINERT.equals(behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            var papirSøknad = hoveddokumentJournalMetadata.getDokumentvarianter().stream().filter(j -> !ArkivFilType.XML.equals(ArkivFilType.fraKode(j.getFiltype()))).findFirst();
            papirSøknad.ifPresent(journalMetadata -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(hoveddokumentJournalMetadata, journalpostId, historikkinnslag, linkTekst)));
        }
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

        leggTilHistorikkinnslagDokumentlinker(BehandlingType.UDEFINERT, journalpostId, historikkinnslag);

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDLEGG_MOTTATT);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();
        builder.medHendelse(historikkinnslagType);
        if (frist != null) {
            builder.medHendelse(historikkinnslagType, frist.toLocalDate());
        }
        if (!Venteårsak.UDEFINERT.equals(venteårsak)) {
            builder.medÅrsak(venteårsak);
        }
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
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
