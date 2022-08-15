package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;

@Dependent
public class HentPerioderTilVurderingTjeneste {

    private SøknadsperiodeTjeneste søknadsperiodeTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    @Inject
    public HentPerioderTilVurderingTjeneste(
            SøknadsperiodeTjeneste søknadsperiodeTjeneste,
            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.søknadsperiodeTjeneste = søknadsperiodeTjeneste;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;

    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingUtenUbesluttet(Behandling behandling) {
        BehandlingReferanse referanse = BehandlingReferanse.fra(behandling);
        var søknadsperioder = TidslinjeUtil.tilTidslinjeKomprimert(finnSykdomsperioder(referanse));

        return fjernTrukkedePerioder(referanse);
    }

    public NavigableSet<DatoIntervallEntitet> hentPerioderTilVurderingMedUbesluttet(Behandling behandling, Optional<DatoIntervallEntitet> utvidetPeriodeSomFølgeAvDødsfall) {
        BehandlingReferanse referanse = BehandlingReferanse.fra(behandling);
        var datoer = søknadsperiodeTjeneste.utledFullstendigPeriode(behandling.getId());

        var søknadsperioder = TidslinjeUtil.tilTidslinjeKomprimert(datoer);
        if (utvidetPeriodeSomFølgeAvDødsfall.isPresent()) {
            søknadsperioder = søknadsperioder.combine(new LocalDateSegment<>(utvidetPeriodeSomFølgeAvDødsfall.get().toLocalDateInterval(), true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return fjernTrukkedePerioder(referanse);
    }

    private TreeSet<DatoIntervallEntitet> fjernTrukkedePerioder(BehandlingReferanse referanse, LocalDateTimeline<Boolean> søknadsperioder) {
        final LocalDateTimeline<Boolean> trukkedeKrav = hentTrukkedeKravTidslinje(referanse);
        return TidslinjeUtil.kunPerioderSomIkkeFinnesI(søknadsperioder, trukkedeKrav).stream()
            .map(s -> DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private LocalDateTimeline<Boolean> hentTrukkedeKravTidslinje(BehandlingReferanse referanse) {
        return TidslinjeUtil.toLocalDateTimeline(søknadsperiodeTjeneste.hentKravperioder(referanse.getFagsakId(), referanse.getBehandlingId())
            .stream()
            .filter(SøknadsperiodeTjeneste.Kravperiode::isHarTrukketKrav)
            .map(SøknadsperiodeTjeneste.Kravperiode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new)));
    }

    private NavigableSet<DatoIntervallEntitet> finnSykdomsperioder(BehandlingReferanse referanse) {
        VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = perioderTilVurderingTjeneste(referanse);
        var definerendeVilkår = perioderTilVurderingTjeneste.definerendeVilkår();
        final var resultat = new TreeSet<DatoIntervallEntitet>();
        for (VilkårType vilkårType : definerendeVilkår) {
            final var periode = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), vilkårType);
            resultat.addAll(periode);
        }
        return resultat;
    }

    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste(BehandlingReferanse behandlingReferanse) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
    }
}
