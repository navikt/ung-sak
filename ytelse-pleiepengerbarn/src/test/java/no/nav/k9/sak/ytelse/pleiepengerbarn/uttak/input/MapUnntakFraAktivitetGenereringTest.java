package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.Saksnummer;

class MapUnntakFraAktivitetGenereringTest {

    @Test
    void skal_håndtere_tom_string() {
        var saksnummerSetMap = MapUnntakFraAktivitetGenerering.mapUnntak("");

        assertThat(saksnummerSetMap).isEmpty();
    }

    @Test
    void skal_håndtere_null_string() {
        var saksnummerSetMap = MapUnntakFraAktivitetGenerering.mapUnntak(null);

        assertThat(saksnummerSetMap).isEmpty();
    }

    @Test
    void skal_parse_saksnummer_og_datoer_separat_ved_en_sak_og_en_dato() {
        var saksnummerSetMap = MapUnntakFraAktivitetGenerering.mapUnntak("ASDF|2021-03-01");

        assertThat(saksnummerSetMap).containsEntry(new Saksnummer("ASDF"), Set.of(LocalDate.parse("2021-03-01")));
    }

    @Test
    void skal_parse_saksnummer_og_datoer_separat_ved_en_sak_og_flere_dato() {
        var saksnummerSetMap = MapUnntakFraAktivitetGenerering.mapUnntak("ASDF|2021-03-01, 2021-05-01");

        assertThat(saksnummerSetMap).containsEntry(new Saksnummer("ASDF"), Set.of(LocalDate.parse("2021-03-01"), LocalDate.parse("2021-05-01")));
    }

    @Test
    void skal_parse_saksnummer_og_datoer_separat_ved_flere_saker() {
        var saksnummerSetMap = MapUnntakFraAktivitetGenerering.mapUnntak("ASDF|2021-03-01, 2021-05-01;GHJK|2021-02-01");

        assertThat(saksnummerSetMap).containsEntry(new Saksnummer("ASDF"), Set.of(LocalDate.parse("2021-03-01"), LocalDate.parse("2021-05-01")));
        assertThat(saksnummerSetMap).containsEntry(new Saksnummer("GHJK"), Set.of(LocalDate.parse("2021-02-01")));
    }
}
