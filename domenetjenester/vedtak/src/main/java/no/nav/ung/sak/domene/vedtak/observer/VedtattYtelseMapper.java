package no.nav.ung.sak.domene.vedtak.observer;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.AnvistAndel;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;

class VedtattYtelseMapper {

    static List<Anvisning> mapAnvisninger(LocalDateTimeline<DagsatsOgUtbetalingsgrad> perioder) {
        if (perioder == null) {
            return List.of();
        }
        return perioder.stream()
            .filter(periode -> periode.getValue().dagsats() > 0)
            .map(VedtattYtelseMapper::map)
            .collect(Collectors.toList());
    }

    private static Anvisning map(LocalDateSegment<DagsatsOgUtbetalingsgrad> segment) {
        final Anvisning anvisning = new Anvisning();
        final Periode p = new Periode();
        p.setFom(segment.getFom());
        p.setTom(segment.getTom());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(segment.getValue().dagsats())));
        anvisning.setUtbetalingsgrad(new Desimaltall(segment.getValue().utbetalingsgrad()));
        anvisning.setAndeler(List.of(new AnvistAndel(null, segment.getValue().dagsats().intValue(), segment.getValue().utbetalingsgrad().intValue(), 0, Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER, null)));
        return anvisning;
    }

}
