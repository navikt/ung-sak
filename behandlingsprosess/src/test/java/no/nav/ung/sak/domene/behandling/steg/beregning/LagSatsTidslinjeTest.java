package no.nav.ung.sak.domene.behandling.steg.beregning;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LagSatsTidslinjeTest {



    @Test
    void skal_gi_høy_sats_dersom_25_år_før_start() {

        LocalDate tjuefemårsdag = LocalDate.now().plusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato,
                false,
                false,
                tjuefemårsdag.plusDays(1)
            )
        );

        assertEquals(2, satsTidslinje.size());
    }

    @Test
    void skal_gi_høy_sats_dersom_25_år_før_dagens_dato_og_etter_start() {

        LocalDate tjuefemårsdag = LocalDate.now().minusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato,
                false,
                false,
                tjuefemårsdag.minusDays(10)
            )
        );

        assertEquals(2, satsTidslinje.size());
    }

    @Test
    void skal_gi_lav_sats_dersom_25_år_etter_start() {

        LocalDate tjuefemårsdag = LocalDate.now().plusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(
                fødselsdato,
                false,
                false,
                tjuefemårsdag.minusDays(1))
        );

        assertEquals(1, satsTidslinje.size());
    }

    @Test
    void skal_gi_høy_sats_dersom_25_år_ved_start() {

        LocalDate tjuefemårsdag = LocalDate.now().plusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(new UtledSatsInput(fødselsdato, false, false, tjuefemårsdag));

        assertEquals(2, satsTidslinje.size());
    }

    @Test
    void skal_gi_hoy_sats_dersom_har_trigger_beregn_hoy_sats() {
        LocalDate fødselsdato = LocalDate.now().minusYears(24);
        LocalDate førsteDagMedYtelse = LocalDate.now();
        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato, true, false, førsteDagMedYtelse)
        );
        // Skal gi både lav og høy sats
        assertEquals(2, satsTidslinje.size());
    }

    @Test
    void skal_gi_hoy_sats_dersom_har_beregnet_hoy_sats_tidligere() {
        LocalDate fødselsdato = LocalDate.now().minusYears(24);
        LocalDate førsteDagMedYtelse = LocalDate.now();
        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato, false, true, førsteDagMedYtelse)
        );
        // Skal gi både lav og høy sats
        assertEquals(2, satsTidslinje.size());
    }

    @Test
    void skal_gi_lav_sats_dersom_ikke_triggere_og_ikke_25_ar() {
        LocalDate fødselsdato = LocalDate.now().minusYears(20);
        LocalDate førsteDagMedYtelse = LocalDate.now();
        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato, false, false, førsteDagMedYtelse)
        );
        // Skal kun gi lav sats
        assertEquals(1, satsTidslinje.size());
    }

    @Test
    void skal_starte_pa_riktig_dato() {
        LocalDate fødselsdato = LocalDate.now().minusYears(20);
        LocalDate førsteDagMedYtelse = LocalDate.now();
        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato, false, false, førsteDagMedYtelse)
        );
        LocalDate forventetStart = fødselsdato.plusYears(Sats.LAV.getFomAlder());
        assertEquals(forventetStart, satsTidslinje.getLocalDateIntervals().first().getFomDato());
    }

    @Test
    void skal_slutte_pa_riktig_dato_for_lav_sats() {
        LocalDate fødselsdato = LocalDate.now().minusYears(20);
        LocalDate førsteDagMedYtelse = LocalDate.now();
        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato, false, false, førsteDagMedYtelse)
        );
        LocalDate forventetSlutt = fødselsdato.plusYears(Sats.LAV.getTilAlder()).minusDays(1);
        assertEquals(forventetSlutt, satsTidslinje.getLocalDateIntervals().first().getTomDato());
    }

    @Test
    void skal_slutte_pa_riktig_dato_for_hoy_sats() {
        LocalDate fødselsdato = LocalDate.now().minusYears(24);
        LocalDate førsteDagMedYtelse = LocalDate.now();
        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(
            new UtledSatsInput(fødselsdato, true, false, førsteDagMedYtelse)
        );
        LocalDate forventetSlutt = fødselsdato.plusYears(Sats.HØY.getTilAlder()).minusDays(1);
        // Hent siste intervall (høy sats)
        assertEquals(forventetSlutt, satsTidslinje.getLocalDateIntervals().last().getTomDato());
    }
}
