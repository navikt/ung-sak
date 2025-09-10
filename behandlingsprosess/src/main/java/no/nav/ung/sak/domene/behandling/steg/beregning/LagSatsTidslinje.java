package no.nav.ung.sak.domene.behandling.steg.beregning;

import static no.nav.ung.sak.behandlingslager.ytelse.sats.Sats.HØY;
import static no.nav.ung.sak.behandlingslager.ytelse.sats.Sats.LAV;

import java.time.LocalDate;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;

public class LagSatsTidslinje {

    /**
     * Lager en tidslinje for sats basert på inputparametere.
     * <p>
     * Tidslinjen bestemmer hvilken sats (LAV eller HØY) som gjelder for ulike perioder basert på fødselsdato, første dag med ytelse og om det er trigget beregning for høy sats.
     * Resultattidslinjen bestemmer når LAV og HØY sats gjelder, og er ikke begrenset til når bruker faktisk mottar ytelse.
     *
     * @param input Inndata som inneholder fødselsdato, første dag med ytelse og flagg for beregning av høy sats
     * @return En tidslinje (LocalDateTimeline) med segmenter for LAV og HØY sats
     */
    static LocalDateTimeline<Sats> lagSatsTidslinje(UtledSatsInput input) {
        var førsteMuligeDato = input.fødselsdato().plusYears(LAV.getFomAlder());
        LocalDate tjuefemårsdagen = input.fødselsdato().plusYears(HØY.getFomAlder());
        var datoForEndringAvSats = tjuefemårsdagen;
        // Dersom 25 års dagen er før eller lik første dag med ytelse, så skal det være høy sats fra start
        boolean skalHaHøySatsFraStart = !tjuefemårsdagen.isAfter(input.førsteDagMedYtelse());
        // Dersom tjuefemårsdagen har passert i dag, så skal det være høy sats
        boolean harPassertTjuefemårsdagen = tjuefemårsdagen.isBefore(LocalDate.now());
        var regnUtHøySats = input.harTriggerBeregnHøySats() || input.harBeregnetHøySatsTidligere()|| skalHaHøySatsFraStart || harPassertTjuefemårsdagen;
        if (regnUtHøySats) {
            var sisteMuligeDato = input.fødselsdato().plusYears(HØY.getTilAlder()).minusDays(1);
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
            var sisteMuligeDato = input.fødselsdato().plusYears(LAV.getTilAlder()).minusDays(1);
            return new LocalDateTimeline<>(førsteMuligeDato, sisteMuligeDato, LAV);
        }
    }
}
