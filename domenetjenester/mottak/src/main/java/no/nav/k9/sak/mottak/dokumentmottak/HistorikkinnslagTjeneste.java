package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.dokument.arkiv.saf.SafTjeneste;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.JournalpostQuery;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentInfo;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.VariantFormat;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.typer.JournalpostId;

@Dependent
public class HistorikkinnslagTjeneste {

    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    public static final String INNTEKTSMELDING_BREVKODE = "4936";
    private HistorikkRepository historikkRepository;
    private SafTjeneste safTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository, SafTjeneste safTjeneste) {
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
            var journalpostIdData = safTjeneste.hentJournalpostInfo(new JournalpostQuery(journalpostId.getVerdi()));
            if (journalpostIdData == null || journalpostIdData.getDokumenter().isEmpty()) {
                return;
            }
            var hoveddokumentJournalMetadata = journalpostIdData.getDokumenter().get(0);

            var elektroniskSøknad = Optional.ofNullable(hoveddokumentJournalMetadata)
                .stream()
                .filter(it -> it.getDokumentvarianter().stream()
                    .anyMatch(ta -> Set.of(VariantFormat.ORIGINAL, VariantFormat.FULLVERSJON).contains(ta.getVariantFormat())
                        && ArkivFilType.XML.equals(ArkivFilType.fraKode(ta.getFiltype())))) // Ustrukturerte dokumenter kan ha xml med variantformat SKANNING_META
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

    public void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling) {
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
            .medÅrsak(BehandlingResultatType.MERGET_OG_HENLAGT);
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandling.getId());
        builder.build(historikkinnslag);
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId, DokumentTypeId dokumentTypeId) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        if (dokumentTypeId != null && dokumentTypeId.equals(DokumentTypeId.INNTEKTSMELDING)) {
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

}
