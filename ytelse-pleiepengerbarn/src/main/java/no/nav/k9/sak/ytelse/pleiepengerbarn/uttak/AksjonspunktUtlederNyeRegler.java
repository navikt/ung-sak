package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.TilkommetAktivitetTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@Dependent
class AksjonspunktUtlederNyeRegler {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktUtlederNyeRegler.class);

    private BehandlingRepository behandlingRepository;
    private UttakTjeneste uttakTjeneste;
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste;

    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    private boolean brukDatoNyRegelUttak;

    AksjonspunktUtlederNyeRegler() {
        // for proxy
    }

    @Inject
    public AksjonspunktUtlederNyeRegler(BehandlingRepository behandlingRepository,
                                        UttakTjeneste uttakTjeneste,
                                        UttakNyeReglerRepository uttakNyeReglerRepository,
                                        TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste,
                                        AksjonspunktKontrollRepository aksjonspunktKontrollRepository, @KonfigVerdi(value = "ENABLE_DATO_NY_REGEL_UTTAK", defaultVerdi = "false") boolean brukDatoNyRegelUttak) {
        this.behandlingRepository = behandlingRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.tilkommetAktivitetTjeneste = tilkommetAktivitetTjeneste;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        this.brukDatoNyRegelUttak = brukDatoNyRegelUttak;
    }


    public Optional<AksjonspunktDefinisjon> utledAksjonspunktDatoForNyeRegler(Behandling behandling) {
        if (!brukDatoNyRegelUttak) {
            return Optional.empty();
        }

        final boolean datoHarBlittSatt = uttakNyeReglerRepository.finnDatoForNyeRegler(behandling.getId()).isPresent();
        var eksisterendeAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);

        if (datoHarBlittSatt && eksisterendeAksjonspunkt.isEmpty()) {
            kopierVurderingFraOriginalBehandling(behandling);
            return Optional.empty();
        }

        if (datoHarBlittSatt && eksisterendeAksjonspunkt.isPresent()) {
            // Her har vi aksjonspunkt og dato, trenger ikkje å endre noko
            return Optional.empty();
        }


        var skalHaAksjonspunkt = harAktivitetIkkeYrkesaktivEllerKunYtelse(behandling) || harTilkommmetAktivitet(behandling);

        return skalHaAksjonspunkt
            ? Optional.of(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK)
            : Optional.empty();
    }

    private void kopierVurderingFraOriginalBehandling(Behandling behandling) {
        var originalAksjonspunkt = behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
            .map(b -> b.getAksjonspunktFor(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK))
            .orElseThrow(() -> new IllegalStateException("Forventer at det finnes aksjonspunkt i original behandling dersom dato er satt"));
        var nyttAksjonspunkt = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        aksjonspunktKontrollRepository.setTilUtført(nyttAksjonspunkt, originalAksjonspunkt.getBegrunnelse());
        nyttAksjonspunkt.setAnsvarligSaksbehandler(originalAksjonspunkt.getAnsvarligSaksbehandler());
    }

    private boolean harTilkommmetAktivitet(Behandling behandling) {
        //har ikke satt dato for nye regler i uttak (utleder AP for det her), så kan ikke begrense perioden (derav LocalDate.MIN)
        boolean harTilkommetAktivitet = !tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(behandling.getFagsakId(), LocalDate.MIN).isEmpty();
        log.info("Har {} tilkommet aktivitet", (harTilkommetAktivitet ? "" : "ikke"));
        return harTilkommetAktivitet;
    }

    private boolean harAktivitetIkkeYrkesaktivEllerKunYtelse(Behandling behandling) {
        Uttaksplan uttaksplan = uttakTjeneste.hentUttaksplan(behandling.getUuid(), false);
        return harEnAv(uttaksplan, Set.of(UttakArbeidType.KUN_YTELSE, UttakArbeidType.IKKE_YRKESAKTIV));
    }

    private boolean harEnAv(Uttaksplan uttaksplan, Collection<UttakArbeidType> aktivitettyper) {
        for (UttaksperiodeInfo uttaksperiodeInfo : uttaksplan.getPerioder().values()) {
            for (Utbetalingsgrader utbetalingsgrader : uttaksperiodeInfo.getUtbetalingsgrader()) {
                for (UttakArbeidType aktivitettype : aktivitettyper) {
                    if (utbetalingsgrader.getArbeidsforhold().getType().equals(aktivitettype.getKode())) {
                        log.info("Har aktivitet IY/KY");
                        return true;
                    }
                }
            }
        }
        log.info("Har ikke aktivitet IY/KY");
        return false;
    }

}
