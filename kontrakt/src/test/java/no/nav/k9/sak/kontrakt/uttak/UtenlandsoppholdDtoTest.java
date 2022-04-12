package no.nav.k9.sak.kontrakt.uttak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.uttak.UtenlandsoppholdÅrsak;
import no.nav.k9.kontrakt.JsonUtil;

class UtenlandsoppholdDtoTest {
    @Test
    public void testJsonAnnotasjoner() throws Exception {
        UtenlandsoppholdDto dto = new UtenlandsoppholdDto();
        dto.leggTil(
            LocalDate.of(2022, 1, 1),
            LocalDate.of(2022, 2, 1),
            Landkoder.SWE,
            UtenlandsoppholdÅrsak.BARNET_INNLAGT_I_HELSEINSTITUSJON_FOR_NORSK_OFFENTLIG_REGNING);

        String json = JsonUtil.getJson(dto);
        UtenlandsoppholdDto retur = JsonUtil.fromJson(json, UtenlandsoppholdDto.class);

        assertThat(retur.getPerioder().size()).isEqualTo(1);
        UtenlandsoppholdPeriodeDto periodeDto = retur.getPerioder().get(0);

        assertThat(periodeDto.getPeriode()).isEqualTo(periodeDto.getPeriode());
        assertThat(periodeDto.getÅrsak()).isEqualTo(periodeDto.getÅrsak());
        assertThat(periodeDto.getLandkode()).isEqualTo(periodeDto.getLandkode());
        assertThat(periodeDto.getRegion()).isEqualTo(periodeDto.getRegion());
    }
}
