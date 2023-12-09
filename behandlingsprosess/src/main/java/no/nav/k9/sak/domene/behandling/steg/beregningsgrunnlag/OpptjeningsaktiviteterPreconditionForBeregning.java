package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@Dependent
public class OpptjeningsaktiviteterPreconditionForBeregning {

    private static final Logger logger = LoggerFactory.getLogger(OpptjeningsaktiviteterPreconditionForBeregning.class);
    private static final Set<VilkårUtfallMerknad> MIDLERTIDIG_INAKTIV_KODER = Set.of(VilkårUtfallMerknad.VM_7847_A, VilkårUtfallMerknad.VM_7847_B);

    private final VilkårResultatRepository vilkårResultatRepository;
    private final OpptjeningRepository opptjeningRepository;

    private final Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    @Inject
    public OpptjeningsaktiviteterPreconditionForBeregning(VilkårResultatRepository vilkårResultatRepository, OpptjeningRepository opptjeningRepository,
                                                          @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste, VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
    }

    public void sjekkOpptjeningsaktiviter(BehandlingReferanse behandlingReferanse) {
        Optional<OpptjeningResultat> opptjeningsresultat = opptjeningRepository.finnOpptjening(behandlingReferanse.getBehandlingId());
        if (opptjeningsresultat.isEmpty()) {
            logger.warn("Har ikke noe opptjeningsresultat");
            return;
        }

        var ikkeAvslåttePerioder = finnPerioder(behandlingReferanse);

        Vilkår opptjeningsvilkåret = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();
        for (var vilkårPeriode : ikkeAvslåttePerioder) {
            var opptjeningVilkårsvurdering = opptjeningsvilkåret.finnPeriodeForSkjæringstidspunkt(vilkårPeriode.getSkjæringstidspunkt());
            var opptjening = opptjeningsresultat.get().finnOpptjening(vilkårPeriode.getSkjæringstidspunkt())
                .orElseThrow(() -> new IllegalStateException("Fant ikke opptjening for skjæringstidspunkt " + vilkårPeriode.getSkjæringstidspunkt()));
            if (!MIDLERTIDIG_INAKTIV_KODER.contains(opptjeningVilkårsvurdering.getMerknad())) {
                sjekkHarAktivitetIHelePerioden(opptjening);
            }

            if (harIkkeInnhentetSigrunForAlleÅr(vilkårPeriode.getSkjæringstidspunkt()) && skalBrukeSigruninntekt(opptjening, vilkårPeriode.getSkjæringstidspunkt(), opptjeningVilkårsvurdering.getMerknad())) {
                throw new IllegalStateException("Kan ikke beregne for status Midlertidig inaktiv eller SN for skjæringstidspunkt før 2019. Sjekk at søknadsperiode er riktig og håndter ved overstyring.");
            }
        }
    }

    private NavigableSet<PeriodeTilVurdering> finnPerioder(BehandlingReferanse behandlingReferanse) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(behandlingReferanse.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var filter = vilkårPeriodeFilterProvider.getFilter(behandlingReferanse);
        filter.ignorerAvslåttePerioder();
        return filter.filtrerPerioder(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }


    private static boolean harIkkeInnhentetSigrunForAlleÅr(LocalDate skjæringstidspunkt) {
        return skjæringstidspunkt.getYear() < 2019;
    }

    private static boolean skalBrukeSigruninntekt(Opptjening opptjening, LocalDate skjæringstidspunkt, VilkårUtfallMerknad opptjeningVilkårsmerknad) {
        return (opptjeningVilkårsmerknad != null && MIDLERTIDIG_INAKTIV_KODER.contains(opptjeningVilkårsmerknad)) || harNæringVedSkjæringstidspunkt(opptjening, skjæringstidspunkt);
    }

    private static boolean harNæringVedSkjæringstidspunkt(Opptjening opptjening, LocalDate skjæringstidspunkt) {
        return opptjening.getOpptjeningAktivitet().stream()
            .anyMatch(a ->
                a.getAktivitetType().equals(OpptjeningAktivitetType.NÆRING) &&
                    !a.getFom().isAfter(skjæringstidspunkt.minusDays(1)) &&
                    !a.getTom().isBefore(skjæringstidspunkt.minusDays(1)));
    }

    private void sjekkHarAktivitetIHelePerioden(Opptjening opptjening) {
        LocalDateTimeline<Boolean> opptjeningsperiode = new LocalDateTimeline<>(opptjening.getFom(), opptjening.getTom(), true);
        LocalDateTimeline<Boolean> harAktivitetTidslinje = new LocalDateTimeline<>(opptjening.getOpptjeningAktivitet().stream()
            .map(oa -> new LocalDateSegment<>(oa.getFom(), oa.getTom(), true))
            .toList(), StandardCombinators::alwaysTrueForMatch);

        LocalDateTimeline<Boolean> manglerAktivitetTidslinje = Hjelpetidslinjer.fjernHelger(opptjeningsperiode.disjoint(harAktivitetTidslinje));
        if (!manglerAktivitetTidslinje.isEmpty()) {
            logger.warn("Opptjening mangler aktiviet for {}", manglerAktivitetTidslinje.stream().map(LocalDateSegment::getLocalDateInterval).toList());
        }
    }

}
