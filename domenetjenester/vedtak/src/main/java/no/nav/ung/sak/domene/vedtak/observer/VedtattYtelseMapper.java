package no.nav.ung.sak.domene.vedtak.observer;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.AnvistAndel;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelsePeriode;

class VedtattYtelseMapper {

    static List<Anvisning> mapAnvisninger(List<TilkjentYtelsePeriode> perioder) {
        if (perioder == null) {
            return List.of();
        }
        return perioder.stream()
            .filter(periode -> periode.dagsats()  > 0)
            .map(VedtattYtelseMapper::map)
            .collect(Collectors.toList());
    }

    private static Anvisning map(TilkjentYtelsePeriode periode) {
        final Anvisning anvisning = new Anvisning();
        final Periode p = new Periode();
        p.setFom(periode.periode().getFomDato());
        p.setTom(periode.periode().getTomDato());
        anvisning.setPeriode(p);
        anvisning.setDagsats(new Desimaltall(new BigDecimal(periode.dagsats())));
        anvisning.setUtbetalingsgrad(new Desimaltall(periode.utbetalingsgrad()));
        anvisning.setAndeler(List.of(new AnvistAndel(null, periode.dagsats().intValue(), periode.utbetalingsgrad().intValue(), 0, Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER, null)));
        return anvisning;
    }

}
