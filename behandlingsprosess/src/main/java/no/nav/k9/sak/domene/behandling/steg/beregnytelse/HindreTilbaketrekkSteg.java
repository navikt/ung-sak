package no.nav.k9.sak.domene.behandling.steg.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.HINDRE_TILBAKETREKK;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.KopierFeriepenger;

@BehandlingStegRef(value = HINDRE_TILBAKETREKK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class HindreTilbaketrekkSteg implements BehandlingSteg {
    private static Logger log = LoggerFactory.getLogger(HindreTilbaketrekkSteg.class);
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    HindreTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkSteg(BehandlingRepositoryProvider repositoryProvider,
                                  @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            throw new IllegalArgumentException("Tilbaketrekk ikke støttet for førstegangsbehandling");
        }

        BehandlingBeregningsresultatEntitet aggregatTY = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        // Valget kopieres alltid fra forrige behandling, se VurderTilbaketrekkSteg
        if (aggregatTY.skalHindreTilbaketrekk().orElse(false)) {
            BeregningsresultatEntitet revurderingTY = beregningsresultatRepository.hentBgBeregningsresultat(behandlingId).orElseThrow();
            var utbetaltTY = kopierUtbetaltResultatFraForrigeBehandlingUtenforVurdertePerioder(behandling, revurderingTY);
            KopierFeriepenger.kopierFraTil(behandlingId, revurderingTY, utbetaltTY);
            log.info("Skal forhindre tilbaketrekk, kopierer utbetalt beregningsresultat");
            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetaltTY);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    /** Kopierer resultat fra utbetalt beregningsresultat for perioder som ikke er vurdert i beregning.
     * Disse periodene skal få det resultatet som ble gjort i den opprinnelige tilbaketrekklogikken fra dette steget. Logikken er fjernet for å begrense teknisk gjeld, men for å unngå tilbakekrevingssaker kopierer vi vedtaket som ble gjort i den opprinnelige løsningen.
     * Denne problemstillingen håndteres i dag i VurderRefusjonBeregningsgrunnlagSteg der saksbehandler velger om refusjon som har kommet inn for sent skal bli med i beregningsgrunnlaget.
     *
     * @param behandling Behandling
     * @param revurderingTY Aktivt beregningsresultat fra denne behandlingen
     * @return Sammenslått resultat av nytt beregningsresultat og forrige utbetalte beregningsresultat
     */
    private BeregningsresultatEntitet kopierUtbetaltResultatFraForrigeBehandlingUtenforVurdertePerioder(Behandling behandling, BeregningsresultatEntitet revurderingTY) {
        var nyUtbetaltAndelTidslinje = finnUtbetalteAndelerTidslinje(behandling, revurderingTY);

        BeregningsresultatEntitet utbetaltTY = BeregningsresultatEntitet.builder()
            .medRegelSporing(revurderingTY.getRegelSporing())
            .medRegelInput(revurderingTY.getRegelInput())
            .build();

        var forventedePerioder = revurderingTY.getBeregningsresultatPerioder().stream().map(BeregningsresultatPeriode::getPeriode).collect(Collectors.toSet());
        nyUtbetaltAndelTidslinje.toSegments().forEach(s -> {
            valdiderIngenEndretPeriode(s, forventedePerioder);
            var periode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(s.getFom(), s.getTom()).build(utbetaltTY);
            s.getValue().stream().map(BeregningsresultatAndel::builder).forEach(builder -> builder.buildFor(periode));
        });
        return utbetaltTY;
    }

    private static void valdiderIngenEndretPeriode(LocalDateSegment<List<BeregningsresultatAndel>> s, Set<DatoIntervallEntitet> forventedePerioder) {
        if (!forventedePerioder.contains(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))) {
            throw new IllegalStateException("Ingen nye periodesplitter skal oppstå i dette steget.");
        }
    }

    private LocalDateTimeline<List<BeregningsresultatAndel>> finnUtbetalteAndelerTidslinje(Behandling behandling, BeregningsresultatEntitet revurderingTY) {
        var tidslinjeTilVurdering = finnTidslinjeSomRevurderes(behandling);
        var forrigeUtbetalteGrunnlag = beregningsresultatRepository.hentBeregningsresultatAggregat(behandling.getOriginalBehandlingId().orElseThrow())
            .map(BehandlingBeregningsresultatEntitet::getUtbetBeregningsresultat)
            .orElseThrow(() -> new IllegalStateException("Forventer å finne et utbetalt grunnlag i forrige behandling dersom det har blitt tatt valg om å hindre tilbakekreving"));
        var andelTidslinjeForrigeUtbetaling = forrigeUtbetalteGrunnlag.getBeregningsresultatAndelTimeline();
        var andelTidslinjeGjeldende = revurderingTY.getBeregningsresultatAndelTimeline();
        var tidslinjeSomSkalVidereføres = andelTidslinjeForrigeUtbetaling.disjoint(tidslinjeTilVurdering);

        log.info("Viderefører resultat fra utbetalt beregningsgrunnlag for følgende perioder:" + tidslinjeSomSkalVidereføres.getLocalDateIntervals());

        return tidslinjeSomSkalVidereføres
            .crossJoin(andelTidslinjeGjeldende, StandardCombinators::coalesceLeftHandSide);
    }

    private LocalDateTimeline<Boolean> finnTidslinjeSomRevurderes(Behandling behandling) {
        var perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType());
        var perioderTilVurderingIBeregning = perioderTilVurdering.utled(behandling.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurderingIBeregning);
    }

}
