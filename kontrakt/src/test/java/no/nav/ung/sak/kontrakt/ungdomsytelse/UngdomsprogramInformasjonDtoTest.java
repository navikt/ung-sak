package no.nav.ung.sak.kontrakt.ungdomsytelse;

import no.nav.ung.kontrakt.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UngdomsprogramInformasjonDtoTest {

    @Test
    void skal_deserialisere_og_serialisere_riktig() throws IOException {
        UngdomsprogramInformasjonDto dto = new UngdomsprogramInformasjonDto(LocalDate.of(2025,4,30), LocalDate.of(2025,4,30), 20);

        final var json = JsonUtil.getJson(dto);

        assertThat(json).isEqualTo("""
            {
              "maksdatoForDeltakelse" : "2025-04-30",
              "opphørsdato" : "2025-04-30",
              "antallDagerBruktForTilkjentePerioder" : 20
            }""");

        final var fromJson = JsonUtil.fromJson(json, UngdomsprogramInformasjonDto.class);

        assertThat(fromJson).isEqualTo(dto);

    }



}
