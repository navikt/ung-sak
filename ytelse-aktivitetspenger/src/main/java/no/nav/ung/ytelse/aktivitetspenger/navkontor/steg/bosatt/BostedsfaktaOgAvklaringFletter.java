package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsinformasjonFraSøknad;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Fletter bostedsfakta fra søknaden med saksbehandlers avklaringer. Fakta og avklaring lagres i hvert sitt grunnlag
 */
public final class BostedsfaktaOgAvklaringFletter {

    private BostedsfaktaOgAvklaringFletter() {
    }

    public static LocalDateTimeline<BostedsfaktaOgAvklaring> flettMedForeslåtteAvklaringer(
        LocalDateTimeline<BostedsinformasjonFraSøknad> søknadsfakta,
        Collection<VilkårPeriodeAvklaring> foreslåtteAvklaringer) {

        return søknadsfakta.mapValue(BostedsfaktaOgAvklaring::fraSøknad)
            .combine(tilTidslinje(foreslåtteAvklaringer),
                (di, fakta, foreslått) -> new LocalDateSegment<>(di,
                    fakta.getValue().medForeslåttAvklaring(foreslått == null ? null : foreslått.getValue())),
                LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    public static LocalDateTimeline<BostedsfaktaOgAvklaring> flettMedAlleAvklaringer(
        LocalDateTimeline<BostedsinformasjonFraSøknad> søknadsfakta,
        Collection<VilkårPeriodeAvklaring> foreslåtteAvklaringer,
        Collection<VilkårPeriodeAvklaring> ferdigstilteAvklaringer) {

        return flettMedForeslåtteAvklaringer(søknadsfakta, foreslåtteAvklaringer)
            .combine(tilTidslinje(ferdigstilteAvklaringer),
                (di, fakta, ferdigstilt) -> new LocalDateSegment<>(di,
                    fakta.getValue().medFerdigstiltAvklaring(ferdigstilt == null ? null : ferdigstilt.getValue())),
                LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private static LocalDateTimeline<VilkårPeriodeAvklaring> tilTidslinje(Collection<VilkårPeriodeAvklaring> avklaringer) {
        return new LocalDateTimeline<>(avklaringer.stream()
            .map(avklaring -> new LocalDateSegment<>(
                avklaring.getPeriode().getFomDato(),
                avklaring.getPeriode().getTomDato(),
                avklaring))
            .collect(Collectors.toList()));
    }
}
