package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.fagsak.SakInfotrygdMigrering;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class RelevantPeriodeUtleder {

    private VilkårResultatRepository vilkårResultatRepository;
    private FagsakRepository fagsakRepository;


    public RelevantPeriodeUtleder() {
    }

    @Inject
    public RelevantPeriodeUtleder(VilkårResultatRepository vilkårResultatRepository, FagsakRepository fagsakRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.fagsakRepository = fagsakRepository;
    }

    public DatoIntervallEntitet utledRelevantPeriode(BehandlingReferanse referanse, DatoIntervallEntitet periode) {
        var tidslinje = utledTidslinje(referanse);
        return utledRelevantPeriode(tidslinje, periode, true);
    }

    public Set<Inntektsmelding> utledRelevanteInntektsmeldinger(Set<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet relevantPeriode) {
        return inntektsmeldinger.stream()
            .filter(im -> im.getStartDatoPermisjon().isPresent() && relevantPeriode.inkluderer(im.getStartDatoPermisjon().orElseThrow()))
            .collect(Collectors.toSet());
    }

    private LocalDateTimeline<Boolean> utledTidslinje(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (vilkårene.isEmpty()) {
            return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true)));
        }
        var vilkåret = vilkårene.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        var stpMigrertFraInfotrygd = fagsakRepository.hentSakInfotrygdMigreringer(referanse.getFagsakId()).stream()
            .map(SakInfotrygdMigrering::getSkjæringstidspunkt)
            .min(Comparator.naturalOrder());

        return vilkåret.map(vilkår -> new LocalDateTimeline<>(vilkår.getPerioder().stream()
                .map(VilkårPeriode::getPeriode)
                .map(DatoIntervallEntitet::toLocalDateInterval)
                .map(it -> utvidPeriodeForPeriodeFraInfotrygd(it, stpMigrertFraInfotrygd))
                .collect(Collectors.toList()), StandardCombinators::coalesceRightHandSide))
            .orElseGet(() -> new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true))));
    }

    DatoIntervallEntitet utledRelevantPeriode(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet periode) {
        return utledRelevantPeriode(tidslinje, periode, true);
    }

    private DatoIntervallEntitet utledRelevantPeriode(LocalDateTimeline<Boolean> tidslinje, DatoIntervallEntitet periode, boolean justerStart) {
        DatoIntervallEntitet orginalRelevantPeriode = periode;
        if (justerStart) {
            orginalRelevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato().minusWeeks(4), periode.getTomDato().plusWeeks(4));
        }

        if (tidslinje.isEmpty()) {
            return orginalRelevantPeriode;
        }
        var intersection = tidslinje.intersection(new LocalDateInterval(orginalRelevantPeriode.getFomDato(), orginalRelevantPeriode.getTomDato()));
        if (intersection.isEmpty()) {
            return orginalRelevantPeriode;
        }
        var relevantPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(intersection.getMinLocalDate().minusWeeks(4), intersection.getMaxLocalDate().plusWeeks(4));

        if (orginalRelevantPeriode.equals(relevantPeriode)) {
            return relevantPeriode;
        }

        return utledRelevantPeriode(tidslinje, relevantPeriode, false);
    }

    /**
     * I tilfelle der vilkårsperioden er migrert fra infotrygd må vi utvide relevant periode for at inntektsmeldinger lenger tilbake i tid skal vurderes som relevante.
     * Inntektsmeldinger for perioder fra infotrygd vil ha opprinnelig skjæringstidspunkt oppgitt i inntektsmeldingen og ikke i skjæringstidspunktet i k9-sak.
     * Vi sier her at vi ser på inntektsmeldinger som er 2 år og 4 mnd gamle.
     *
     * @param opprinneligVilkårsperiode Opprinnelig vilkårsperiode
     * @param stpMigrertFraInfotrygd    Skjæringstidspunkt som er migrert fra infotrygd
     * @return LocaldateSegment for relevant periode for vilkårsperiode
     */
    private LocalDateSegment<Boolean> utvidPeriodeForPeriodeFraInfotrygd(LocalDateInterval opprinneligVilkårsperiode, Optional<LocalDate> stpMigrertFraInfotrygd) {
        if (stpMigrertFraInfotrygd.map(opprinneligVilkårsperiode.getFomDato()::equals).orElse(false)) {
            var periode = new LocalDateInterval(opprinneligVilkårsperiode.getFomDato().minusYears(2), opprinneligVilkårsperiode.getTomDato());
            return new LocalDateSegment<>(periode, true);
        }
        return new LocalDateSegment<>(opprinneligVilkårsperiode, true);
    }


}
