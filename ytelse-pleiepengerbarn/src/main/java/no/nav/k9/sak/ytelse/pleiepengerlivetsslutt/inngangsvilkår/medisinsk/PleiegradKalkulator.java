package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import java.time.LocalDate;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.LivetsSluttfaseDokumentasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.Pleielokasjon;

public class PleiegradKalkulator {

    private static final LocalDate DATO_FOR_NY_MAX_PLEIEGRAD = LocalDate.of(2023, 1, 1);

    static LocalDateTimeline<Pleiegrad> MAX_PLEIEGRAD = new LocalDateTimeline<>(List.of(
        new LocalDateSegment<>(LocalDate.MIN, DATO_FOR_NY_MAX_PLEIEGRAD.minusDays(1), Pleiegrad.LIVETS_SLUTT_TILSYN),
        new LocalDateSegment<>(DATO_FOR_NY_MAX_PLEIEGRAD, LocalDate.MAX, Pleiegrad.LIVETS_SLUTT_TILSYN_FOM2023)));

    static LocalDateTimeline<Pleiegrad> regnUtPleiegrad(MedisinskVilkårResultat vilkårresultat) {
        LocalDateTimeline<LivetsSluttfaseDokumentasjon> tidslinjeDokumentertFravær = vilkårresultat.tidslinjeLivetsSluttfaseDokumentasjon().filterValue(p -> p == LivetsSluttfaseDokumentasjon.DOKUMENTERT);
        LocalDateTimeline<LivetsSluttfaseDokumentasjon> tidslinjeIkkeDokumentertFravær = vilkårresultat.tidslinjeLivetsSluttfaseDokumentasjon().disjoint(tidslinjeDokumentertFravær);

        LocalDateTimeline<Pleiegrad> pleiegradFratrekkInnleggelse = vilkårresultat.tidslinjePleielokasjon().filterValue(Pleielokasjon.INNLAGT::equals).mapValue(v -> Pleiegrad.INGEN);

        return MAX_PLEIEGRAD.intersection(tidslinjeDokumentertFravær)
            .combine(tidslinjeIkkeDokumentertFravær.mapValue(v -> Pleiegrad.INGEN), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .combine(pleiegradFratrekkInnleggelse, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }
}
