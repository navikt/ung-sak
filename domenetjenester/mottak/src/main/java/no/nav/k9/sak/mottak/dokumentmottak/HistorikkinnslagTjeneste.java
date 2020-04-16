package no.nav.k9.sak.mottak.dokumentmottak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.dokument.VariantFormat;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.dokument.arkiv.journal.JournalMetadata;
import no.nav.k9.sak.dokument.arkiv.journal.JournalTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.typer.JournalpostId;

@Dependent
public class HistorikkinnslagTjeneste {

    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    private HistorikkRepository historikkRepository;
    private JournalTjeneste journalTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository, JournalTjeneste journalTjeneste) {
        this.historikkRepository = historikkRepository;
        this.journalTjeneste = journalTjeneste;
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
            List<JournalMetadata> journalMetadataListe = journalTjeneste.hentMetadata(journalpostId);

            List<JournalMetadata> hoveddokumentJournalMetadata = journalMetadataListe.stream().filter(JournalMetadata::getErHoveddokument).collect(Collectors.toList());

            Optional<JournalMetadata> elektroniskSøknad = hoveddokumentJournalMetadata.stream()
                .filter(it -> VariantFormat.ORIGINAL.equals(it.getVariantFormat())
                    || VariantFormat.FULLVERSJON.equals(it.getVariantFormat())) // Ustrukturerte dokumenter kan ha xml med variantformat SKANNING_META
                .filter(it -> ArkivFilType.XML.equals(it.getArkivFilType())).findFirst();

            leggTilSøknadDokumentLenke(behandlingType, journalpostId, historikkinnslag, dokumentLinker, hoveddokumentJournalMetadata, elektroniskSøknad);
            journalMetadataListe.stream().filter(j -> !j.getErHoveddokument()).forEach(journalMetadata -> dokumentLinker
                .add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, VEDLEGG)));
        }

        historikkinnslag.setDokumentLinker(dokumentLinker);
    }

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag, List<HistorikkinnslagDokumentLink> dokumentLinker,
                                            List<JournalMetadata> hoveddokumentJournalMetadata, Optional<JournalMetadata> elektroniskSøknad) {
        if (elektroniskSøknad.isPresent()) {
            final JournalMetadata journalMetadata = elektroniskSøknad.get();
            String linkTekst = journalMetadata.getDokumentType().equals(DokumentTypeId.INNTEKTSMELDING) ? INNTEKTSMELDING : SØKNAD; // NOSONAR
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, linkTekst));
        } else {
            String linkTekst = BehandlingType.UDEFINERT.equals(behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            Optional<JournalMetadata> papirSøknad = hoveddokumentJournalMetadata.stream().filter(j -> !ArkivFilType.XML.equals(j.getArkivFilType())).findFirst();
            papirSøknad.ifPresent(journalMetadata -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(journalMetadata, journalpostId, historikkinnslag, linkTekst)));
        }
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(JournalMetadata journalMetadata, JournalpostId journalpostId, Historikkinnslag historikkinnslag, String linkTekst) {
        HistorikkinnslagDokumentLink historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(journalMetadata.getDokumentId());
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
