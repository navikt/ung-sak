package no.nav.ung.sak.domene.behandling.steg.beregning;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LagSatsTidslinjeTest {

    @Test
    void skal_gi_høy_sats_dersom_25_år_før_start() {

        LocalDate tjuefemårsdag = LocalDate.now().plusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(fødselsdato, LocalDate.now(), false, tjuefemårsdag.plusDays(1));

        assertEquals(2, satsTidslinje.size());
    }

    @Test
    void skal_gi_lav_sats_dersom_25_år_etter_start() {

        LocalDate tjuefemårsdag = LocalDate.now().plusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(fødselsdato, LocalDate.now(), false, tjuefemårsdag.minusDays(1));

        assertEquals(1, satsTidslinje.size());
    }

    @Test
    void skal_gi_høy_sats_dersom_25_år_ved_start() {

        LocalDate tjuefemårsdag = LocalDate.now().plusDays(10);
        LocalDate fødselsdato = tjuefemårsdag.minusYears(25);

        LocalDateTimeline<Sats> satsTidslinje = LagSatsTidslinje.lagSatsTidslinje(fødselsdato, LocalDate.now(), false, tjuefemårsdag);

        assertEquals(2, satsTidslinje.size());
    }

}
