package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.opptjening.MellomliggendeHelgUtleder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class MapYtelsesstidslinjerForPermisjonvalidering {

    private final MellomliggendeHelgUtleder mellomliggendeHelgUtleder = new MellomliggendeHelgUtleder();


    public Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> utledYtelsesTidslinjerForValideringAvPermisjoner(YtelseFilter ytelseFilter) {
        var ytelsesperioder = ytelseFilter.getFiltrertYtelser()
            .stream().flatMap(this::mapYtelseperioder).toList();
        var gruppertPåYtelse = ytelsesperioder.stream()
            .collect(Collectors.groupingBy(Ytelseperiode::ytelseType));
        var timelinePerYtelse = new HashMap<OpptjeningAktivitetType, LocalDateTimeline<Boolean>>();

        for (Map.Entry<OpptjeningAktivitetType, List<Ytelseperiode>> entry : gruppertPåYtelse.entrySet()) {
            var segmenter = entry.getValue().stream().map(it -> new LocalDateSegment<>(it.periode().toLocalDateInterval(), true)).collect(Collectors.toSet());
            var timeline = new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch);
            var mellomliggendePerioder = mellomliggendeHelgUtleder.beregnMellomliggendeHelg(timeline);
            timeline = timeline.combine(mellomliggendePerioder, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            timelinePerYtelse.put(entry.getKey(), timeline.compress());
        }

        return timelinePerYtelse;
    }

    private Stream<Ytelseperiode> mapYtelseperioder(Ytelse y) {
        // Bruker vedtaksperioden for foreldrepenger (se https://jira.adeo.no/browse/TSF-2735)
        if (y.getYtelseType().equals(FagsakYtelseType.FORELDREPENGER)) {
            return Stream.of(new Ytelseperiode(MapYtelseperioderTjeneste.mapYtelseType(y), y.getPeriode()));
        } else {
            return y.getYtelseAnvist().stream().map(ya -> new Ytelseperiode(MapYtelseperioderTjeneste.mapYtelseType(y), MapYtelseperioderTjeneste.hentUtDatoIntervall(y, ya)));
        }
    }

    private record Ytelseperiode(OpptjeningAktivitetType ytelseType, DatoIntervallEntitet periode) {
    }


}
