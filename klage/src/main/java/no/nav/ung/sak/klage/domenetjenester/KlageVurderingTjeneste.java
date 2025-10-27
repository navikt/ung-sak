package no.nav.ung.sak.klage.domenetjenester;

import java.time.LocalDate;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.*;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fritekst.FritekstRepository;
import no.nav.ung.sak.formidling.bestilling.VedtaksbrevbestillingTjeneste;
import no.nav.ung.sak.kontrakt.klage.KlageresultatEndretEvent;

@Dependent
public class KlageVurderingTjeneste {

    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private KlageRepository klageRepository;
    private FritekstRepository fritekstRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private VedtaksbrevbestillingTjeneste brevbestillingTjeneste;

    protected KlageVurderingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KlageVurderingTjeneste(
        ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
        FritekstRepository fritekstRepository, BehandlingRepository behandlingRepository,
        KlageRepository klageRepository,
        BehandlingskontrollTjeneste behandlingskontrollTjeneste,
        VedtaksbrevbestillingTjeneste brevbestillingTjeneste
    ) {
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.fritekstRepository = fritekstRepository;
        this.klageRepository = klageRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.brevbestillingTjeneste = brevbestillingTjeneste;
    }

    public void opprettKlageUtredning(Behandling behandling, OrganisasjonsEnhet nyEnhet) {
        if (klageRepository.finnKlageUtredning(behandling.getId()).isPresent()) {
            throw new IllegalStateException("Utviklerfeil: Ikke tilllatt å opprette utredning flere ganger");
        }
        var utredning = KlageUtredningEntitet.builder()
            .medOpprinneligBehandlendeEnhet(nyEnhet.getEnhetId())
            .medKlageBehandling(behandling)
            .build();
        klageRepository.lagre(utredning);
    }

    public void lagreVurdering(Behandling behandling, KlageVurderingAdapter klagevurdering) {
        var oppdatertKlageutredning = byggOgLagreKlageVurdering(behandling, klagevurdering);

        if (klagevurdering.skalOversendesTilNK()) {
            brevbestillingTjeneste.bestillVedtaksbrevFor(behandling);
            oppdaterBehandlingMedNyFrist(behandling);
        }
        oppdaterBehandlingMedBehandlingsresultat(behandling, oppdatertKlageutredning);
    }

    public void mellomlagreVurderingResultat(Behandling behandling, KlageVurderingAdapter adapter) {
        byggOgLagreKlageVurdering(behandling, adapter);
    }

    public void mellomlagreVurderingResultatOgÅpneAksjonspunkt(Behandling behandling, KlageVurderingAdapter adapter) {
        tilbakeførBehandling(behandling);
        byggOgLagreKlageVurdering(behandling, adapter);
    }

    private KlageUtredningEntitet byggOgLagreKlageVurdering(Behandling behandling, KlageVurderingAdapter klagevurdering) {
        var klageutredning = klageRepository.hentKlageUtredning(behandling.getId());
        klageutredning.setKlagevurdering(klagevurdering);
        klageRepository.lagre(klageutredning);

        fritekstRepository.lagre(behandling.getId(), klagevurdering.getKlageVurdertAv(), klagevurdering.getFritekstTilBrev());

        return klageutredning;
    }

    private void tilbakeførBehandling(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, BehandlingStegType.VURDER_KLAGE_FØRSTEINSTANS);
        prosesseringAsynkTjeneste.asynkStartBehandlingProsess(behandling);
    }

    private void oppdaterBehandlingMedNyFrist(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingstidFrist(LocalDate.now().plusWeeks(14));
        behandlingRepository.lagre(behandling, lås);
    }

    public void observerKlageresultatEndret(@Observes KlageresultatEndretEvent event) {
        var behandling = behandlingRepository.hentBehandling(event.behandlingId());
        var utredning = klageRepository.hentKlageUtredning(event.behandlingId());
        oppdaterBehandlingMedBehandlingsresultat(behandling, utredning);
    }

    private void oppdaterBehandlingMedBehandlingsresultat(Behandling behandling, KlageUtredningEntitet klageUtredning) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandling.setBehandlingResultatType(tolkBehandlingResultatType(klageUtredning.hentGjeldendeKlagevurderingType()));
        behandlingRepository.lagre(behandling, lås);
    }

    public static BehandlingResultatType tolkBehandlingResultatType(KlageVurderingType vurdering) {
        return switch (vurdering) {
            case KlageVurderingType.AVVIS_KLAGE -> BehandlingResultatType.KLAGE_AVVIST;
            case KlageVurderingType.MEDHOLD_I_KLAGE -> BehandlingResultatType.KLAGE_MEDHOLD;
            case KlageVurderingType.OPPHEVE_YTELSESVEDTAK -> BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET;
            case KlageVurderingType.STADFESTE_YTELSESVEDTAK -> BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET;
            case KlageVurderingType.HJEMSENDE_UTEN_Å_OPPHEVE -> BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE;
            case KlageVurderingType.TRUKKET -> BehandlingResultatType.KLAGE_TRUKKET;
            case KlageVurderingType.FEILREGISTRERT -> BehandlingResultatType.FEILREGISTRERT;
            case null, default -> BehandlingResultatType.IKKE_FASTSATT;
        };
    }
}
