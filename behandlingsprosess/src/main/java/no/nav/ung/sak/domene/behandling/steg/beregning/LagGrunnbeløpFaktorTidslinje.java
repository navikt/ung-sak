package no.nav.ung.sak.domene.behandling.steg.beregning;

import static no.nav.ung.sak.behandlingslager.ytelse.sats.Sats.HØY;
import static no.nav.ung.sak.behandlingslager.ytelse.sats.Sats.LAV;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;

public class LagGrunnbeløpFaktorTidslinje {

    static LocalDateTimeline<Sats> lagGrunnbeløpFaktorTidslinje(LocalDate fødselsdato, LocalDate beregningsdato, boolean harTriggerBeregnHøySats) {
        var førsteMuligeDato = fødselsdato.plusYears(LAV.getFomAlder()).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        LocalDate tjuefemårsdagen = fødselsdato.plusYears(HØY.getFomAlder());
        var datoForEndringAvSats = tjuefemårsdagen.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        var sisteMuligeDato = fødselsdato.plusYears(HØY.getTomAlder()).with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);

        var regnUtHøySats = harTriggerBeregnHøySats || beregningsdato.isAfter(tjuefemårsdagen);
        if (regnUtHøySats) {
            return new LocalDateTimeline<>(
                List.of(
                    new LocalDateSegment<>(
                        førsteMuligeDato,
                        datoForEndringAvSats.minusDays(1),
                        LAV),
                    new LocalDateSegment<>(
                        datoForEndringAvSats,
                        sisteMuligeDato,
                        HØY)

                ));
        } else {
            return new LocalDateTimeline<>(førsteMuligeDato, sisteMuligeDato, LAV);
        }

    }


}
