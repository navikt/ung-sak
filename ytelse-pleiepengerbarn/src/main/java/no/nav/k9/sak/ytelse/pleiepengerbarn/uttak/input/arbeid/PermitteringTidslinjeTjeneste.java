package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.typer.Stillingsprosent;

class PermitteringTidslinjeTjeneste {

    static LocalDateTimeline<Boolean> mapPermittering(Yrkesaktivitet yrkesaktivitet) {
        var relevantePermitteringer = yrkesaktivitet.getPermisjon().stream()
            .filter(it -> Objects.equals(it.getPermisjonsbeskrivelseType(), PermisjonsbeskrivelseType.PERMITTERING))
            .filter(it -> erStørreEllerLik100Prosent(it.getProsentsats()))
            .map(permisjon -> new LocalDateSegment<>(permisjon.getFraOgMed(), permisjon.getTilOgMed(), true))
            .toList();

        LocalDateTimeline<Boolean> timeline = new LocalDateTimeline<>(relevantePermitteringer, StandardCombinators::alwaysTrueForMatch);

        return timeline.compress();
    }


    static boolean erStørreEllerLik100Prosent(Stillingsprosent prosentsats) {
        return Stillingsprosent.HUNDRED.getVerdi().intValue() <= prosentsats.getVerdi().intValue();
    }

}
