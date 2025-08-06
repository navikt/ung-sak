package no.nav.ung.sak.klage;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.klage.KlageVurdering;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;

@Dependent
public class KlageVurderingTjeneste {

//    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private KlageRepository klageRepository;
    private FritekstRepository fritekstRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    protected KlageVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageVurderingTjeneste(
        //DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                                  ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                  FritekstRepository fritekstRepository, BehandlingRepository behandlingRepository,
                                  KlageRepository klageRepository,
                                  BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
//        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.fritekstRepository = fritekstRepository;
        this.klageRepository = klageRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void opprettKlageUtredning(Behandling behandling, UUID påklagdBehandlingRef, OrganisasjonsEnhet nyEnhet,
                                      BehandlingType påklagdBehandlingType) {
        if (klageRepository.finnKlageUtredning(behandling.getId()).isPresent()) {
            throw new IllegalStateException("Utviklerfeil: Ikke tilllatt å opprette utredning flere ganger");
        }
        var utredning = KlageUtredning.builder()
            .medOpprinneligBehandlendeEnhet(nyEnhet.getEnhetId())
            .medKlageBehandling(behandling)
            .medPåKlagdBehandlingRef(påklagdBehandlingRef)
            .medPåKlagdBehandlingType(påklagdBehandlingType)
            .build();
        klageRepository.lagre(utredning);
    }

    public void lagreFormkrav(Behandling behandling, KlageFormkravAdapter formkrav) {
        klageRepository.hentVurdering(behandling.getId(), formkrav.getKlageVurdertAvKode())
            .ifPresentOrElse(eksisterendeVurdering -> {
                    eksisterendeVurdering.setFormkrav(formkrav);
                    klageRepository.lagre(eksisterendeVurdering);
                },
                () -> lagNyVurdering(behandling, formkrav));
    }

    public void lagreVurdering(Behandling behandling, KlageVurderingAdapter klagevurdering) {
        byggOgLagreKlageVurdering(behandling, klagevurdering);
        if (klagevurdering.skalOversendesTilNK(behandling)) {
//            BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(),
//                DokumentMalType.KLAGE_OVERSENDT_KLAGEINSTANS_DOK,
//                klagevurdering.getFritekstTilBrev());
//            dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);

            oppdaterBehandlingMedNyFrist(behandling);
        }
        oppdaterBehandlingMedBehandlingsresultat(behandling, klagevurdering.getKlageVurdering());
    }

    public void mellomlagreVurderingResultat(Behandling behandling, KlageVurderingAdapter adapter) {
        byggOgLagreKlageVurdering(behandling, adapter);
    }

    public void mellomlagreVurderingResultatOgÅpneAksjonspunkt(Behandling behandling, KlageVurderingAdapter adapter) {
        tilbakeførBehandling(behandling);
        byggOgLagreKlageVurdering(behandling, adapter);
    }

    private void tilbakeførBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.VURDER_KLAGE_FØRSTEINSTANS);
//        prosesseringAsynkTjeneste.asynkProsesserBehandling(behandling);
    }

    private void byggOgLagreKlageVurdering(Behandling behandling, KlageVurderingAdapter adapter) {
        klageRepository.hentVurdering(behandling.getId(), adapter.getKlageVurdertAv())
            .ifPresentOrElse(eksisterendeVurdering -> oppdaterVurdering(eksisterendeVurdering, adapter),
                () -> lagNyVurdering(behandling, adapter));

        fritekstRepository.lagre(behandling.getId(), adapter.getKlageVurdertAv(), adapter.getFritekstTilBrev());
    }

    private void lagNyVurdering(Behandling behandling, KlageVurderingAdapter adapter) {
        KlageVurderingEntitet.Builder klageVurderingResultatBuilder = new KlageVurderingEntitet.Builder()
            .medKlageKlagebehandling(behandling)
            .medKlageVurdertAv(adapter.getKlageVurdertAv())
            .medResultat(new Vurderingresultat(adapter));

        klageRepository.lagre(klageVurderingResultatBuilder.build());
    }

    private void oppdaterVurdering(KlageVurderingEntitet eksisterendeVurdering, KlageVurderingAdapter adapter) {
        Vurderingresultat nyVurdering = new Vurderingresultat(adapter);
        eksisterendeVurdering.setKlageresultat(nyVurdering);
        // if (erGodkjentAvMedunderskriver(eksisterendeVurdering, nyVurdering)) { }   // TODO: Må oppdatere behandlingen hvis behov for ny godkjenning
        klageRepository.lagre(eksisterendeVurdering);
    }

//    private boolean erGodkjentAvMedunderskriver(KlageVurderingEntitet eksisterendeVurdering, Vurderingresultat nyKlageVurdering) {
//        return eksisterendeVurdering.isGodkjentAvMedunderskriver()
//            && eksisterendeVurdering.getKlageresultat().equals(nyKlageVurdering);
//    }

    private void lagNyVurdering(Behandling behandling, KlageFormkravAdapter formkrav) {
        KlageVurderingEntitet klageVurdering = new KlageVurderingEntitet.Builder()
            .medKlageVurdertAv(formkrav.getKlageVurdertAvKode())
            .medKlageKlagebehandling(behandling)
            .medFormkrav(formkrav)
            .build();

        klageRepository.lagre(klageVurdering);
    }

    private void oppdaterBehandlingMedNyFrist(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(LocalDate.now().plusWeeks(14));
        behandlingRepository.lagre(behandling, lås);
    }

    private void oppdaterBehandlingMedBehandlingsresultat(Behandling behandling, KlageVurdering vurdering) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingResultatType(tolkBehandlingResultatType(vurdering));
        behandlingRepository.lagre(behandling, lås);
    }

    public static BehandlingResultatType tolkBehandlingResultatType(KlageVurdering vurdering) {
        return switch (vurdering) {
            case KlageVurdering.AVVIS_KLAGE -> BehandlingResultatType.KLAGE_AVVIST;
            case KlageVurdering.MEDHOLD_I_KLAGE -> BehandlingResultatType.KLAGE_MEDHOLD;
            case KlageVurdering.OPPHEVE_YTELSESVEDTAK -> BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET;
            case KlageVurdering.STADFESTE_YTELSESVEDTAK -> BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET;
            case KlageVurdering.HJEMSENDE_UTEN_Å_OPPHEVE -> BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE;
            case KlageVurdering.TRUKKET -> BehandlingResultatType.KLAGE_TRUKKET;
            case KlageVurdering.FEILREGISTRERT -> BehandlingResultatType.FEILREGISTRERT;
            default -> null;
        };
    }
}
