package no.nav.ung.sak.domene.behandling.steg.beregning;

import static no.nav.ung.sak.behandlingslager.ytelse.sats.Sats.HØY;
import static no.nav.ung.sak.behandlingslager.ytelse.sats.Sats.LAV;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;

public class LagSatsTidslinje {

    static LocalDateTimeline<Sats> lagSatsTidslinje(LocalDate fødselsdato, LocalDate beregningsdato, boolean harTriggerBeregnHøySats, LocalDate førsteDagMedYtelsen) {
        var førsteMuligeDato = fødselsdato.plusYears(LAV.getFomAlder());
        LocalDate tjuefemårsdagen = fødselsdato.plusYears(HØY.getFomAlder());
        var datoForEndringAvSats = tjuefemårsdagen;

        var regnUtHøySats = harTriggerBeregnHøySats || !beregningsdato.isBefore(tjuefemårsdagen) || !tjuefemårsdagen.isAfter(førsteDagMedYtelsen);
        if (regnUtHøySats) {
            var sisteMuligeDato = fødselsdato.plusYears(HØY.getTilAlder()).minusDays(1);
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
            var sisteMuligeDato = fødselsdato.plusYears(LAV.getTilAlder()).minusDays(1);
            return new LocalDateTimeline<>(førsteMuligeDato, sisteMuligeDato, LAV);
        }
    }
}
