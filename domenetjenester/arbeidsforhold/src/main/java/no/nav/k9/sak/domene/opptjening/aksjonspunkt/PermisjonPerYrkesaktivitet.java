package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.typer.Stillingsprosent;

public final class PermisjonPerYrkesaktivitet {

    public static LocalDateTimeline<Boolean> utledPermisjonPerYrkesaktivitet(Yrkesaktivitet yrkesaktivitet,
                                                                             Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse,
                                                                             Collection<DatoIntervallEntitet> vilkårsperiode) {
        List<LocalDateTimeline<Boolean>> aktivPermisjonTidslinjer = yrkesaktivitet.getPermisjon()
            .stream()
            .filter(permisjon -> erStørreEllerLik100Prosent(permisjon.getProsentsats()))
            .map(it -> justerPeriodeEtterYtelse(it, tidslinjePerYtelse, vilkårsperiode))
            .flatMap(Collection::stream)
            .map(permisjon -> new LocalDateTimeline<>(permisjon.toLocalDateInterval(), Boolean.TRUE))
            .toList();
        LocalDateTimeline<Boolean> aktivPermisjonTidslinje = new LocalDateTimeline<>(List.of());
        for (LocalDateTimeline<Boolean> linje : aktivPermisjonTidslinjer) {
            aktivPermisjonTidslinje = aktivPermisjonTidslinje.combine(linje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }
        return aktivPermisjonTidslinje;
    }

    private static Set<DatoIntervallEntitet> justerPeriodeEtterYtelse(Permisjon it, Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse, Collection<DatoIntervallEntitet> vilkårsperioder) {
        if (Objects.equals(it.getPermisjonsbeskrivelseType(), PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER)) {
            var ytelsesTidslinje = tidslinjePerYtelse.getOrDefault(OpptjeningAktivitetType.FORELDREPENGER, new LocalDateTimeline<>(List.of()));
            ytelsesTidslinje = ytelsesTidslinje.crossJoin(tidslinjePerYtelse.getOrDefault(OpptjeningAktivitetType.SVANGERSKAPSPENGER, new LocalDateTimeline<>(List.of())));
            ytelsesTidslinje = ytelsesTidslinje.crossJoin(tidslinjePerYtelse.getOrDefault(OpptjeningAktivitetType.PLEIEPENGER, new LocalDateTimeline<>(List.of())));
            ytelsesTidslinje = ytelsesTidslinje.crossJoin(tidslinjePerYtelse.getOrDefault(OpptjeningAktivitetType.PLEIEPENGER_AV_DAGPENGER, new LocalDateTimeline<>(List.of())));
            var permisjonstidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), true)));
            permisjonstidslinje = permisjonstidslinje.disjoint(ytelsesTidslinje);
            return TidslinjeUtil.tilDatoIntervallEntiteter(permisjonstidslinje.compress());
        } else if (PermisjonsbeskrivelseType.K9_VELFERDSPERMISJON.contains(it.getPermisjonsbeskrivelseType())) {
            var permisjonstidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(it.getFraOgMed(), it.getTilOgMed(), true)));
            // Filtrerer bort overlapp av k9-yteser for velferdspermisjon siden k9-ytelser ofte rapporteres som velferdspermisjon
            for (OpptjeningAktivitetType aktivitetType : OpptjeningAktivitetType.K9_YTELSER) {
                var ytelsesTidslinje = tidslinjePerYtelse.getOrDefault(aktivitetType, new LocalDateTimeline<>(List.of()));
                permisjonstidslinje = permisjonstidslinje.disjoint(ytelsesTidslinje);
            }
            // Filtrerer også bort gjeldende vilkårsperiode for å unngå at tidlig innraportering til aareg gir avslag under saksbehandling
            var vilkårsperiodeTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(vilkårsperioder);
            permisjonstidslinje = permisjonstidslinje.disjoint(vilkårsperiodeTidslinje);
            return TidslinjeUtil.tilDatoIntervallEntiteter(permisjonstidslinje.compress());
        }
        return Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(it.getFraOgMed(), it.getTilOgMed()));
    }

    private static boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }
}
