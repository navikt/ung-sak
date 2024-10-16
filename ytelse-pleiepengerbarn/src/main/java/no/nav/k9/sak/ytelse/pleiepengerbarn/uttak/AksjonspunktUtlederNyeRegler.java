package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.TilkommetAktivitetTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.MapInputTilUttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;

@Dependent
class AksjonspunktUtlederNyeRegler {

    private static final Logger log = LoggerFactory.getLogger(AksjonspunktUtlederNyeRegler.class);

    private BehandlingRepository behandlingRepository;
    private UttakNyeReglerRepository uttakNyeReglerRepository;
    private TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private MapInputTilUttakTjeneste mapInputTilUttakTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    AksjonspunktUtlederNyeRegler() {
        // for proxy
    }

    @Inject
    public AksjonspunktUtlederNyeRegler(BehandlingRepository behandlingRepository,
                                        UttakNyeReglerRepository uttakNyeReglerRepository,
                                        TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste,
                                        AksjonspunktKontrollRepository aksjonspunktKontrollRepository,
                                        SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                        MapInputTilUttakTjeneste mapInputTilUttakTjeneste,
                                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.tilkommetAktivitetTjeneste = tilkommetAktivitetTjeneste;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.mapInputTilUttakTjeneste = mapInputTilUttakTjeneste;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }


    public Optional<AksjonspunktDefinisjon> utledAksjonspunktDatoForNyeRegler(Behandling behandling) {
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

        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        // Vi sjekker Beregningsgrunnlagsvilkåret siden gradering mot ny inntekt kun endres/settes dersom vi vurderer beregning på nytt
        var perioderTilVurdering = periodeTjeneste.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);


        var skalHaAksjonspunkt = harAktivitetIkkeYrkesaktivEllerKunYtelse(behandling, perioderTilVurdering) || harTilkommmetAktivitet(behandling, perioderTilVurdering);

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

    private boolean harTilkommmetAktivitet(Behandling behandling, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        if (perioderTilVurdering.isEmpty()) {
            return false;
        }
        boolean harTilkommetAktivitet = !tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(behandling.getFagsakId(), perioderTilVurdering).isEmpty();
        log.info("Har {} tilkommet aktivitet", (harTilkommetAktivitet ? "" : "ikke"));
        return harTilkommetAktivitet;
    }

    private boolean harAktivitetIkkeYrkesaktivEllerKunYtelse(Behandling behandling, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var uttaksgrunnlag = mapInputTilUttakTjeneste.hentUtOgMapRequestUtenInntektsgradering(BehandlingReferanse.fra(behandling));
        return harEnAv(uttaksgrunnlag, Set.of(UttakArbeidType.KUN_YTELSE, UttakArbeidType.IKKE_YRKESAKTIV), perioderTilVurdering);
    }

    private boolean harEnAv(Uttaksgrunnlag uttaksgrunnlag, Collection<UttakArbeidType> aktivitettyper, NavigableSet<DatoIntervallEntitet> perioderTilVurdering) {
        var harGittType = uttaksgrunnlag.getArbeid().stream().anyMatch(
            a -> overlapperPeriodeMedPeriodeTilVurdering(perioderTilVurdering, a) &&
                aktivitettyper.stream().map(UttakArbeidType::getKode)
                    .anyMatch(a.getArbeidsforhold().getType()::equals)
        );
        if (harGittType) {
            log.info("Har aktivitet IY/KY");
        } else {
            log.info("Har ikke aktivitet IY/KY");
        }
        return harGittType;
    }

    private static boolean overlapperPeriodeMedPeriodeTilVurdering(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Arbeid a) {
        return a.getPerioder().keySet().stream()
            .anyMatch(arbeidsperiode -> perioderTilVurdering.stream().anyMatch(tilVurdering -> DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsperiode.getFom(), arbeidsperiode.getTom()).overlapper(tilVurdering)));
    }

}
