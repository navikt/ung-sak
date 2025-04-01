package no.nav.ung.sak.kontrakt.behandling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingDtoTest {


    @Test
    void sjekk_at_begge_det_finnes_keys_med_og_uten_æøå() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        BehandlingDto dto = new BehandlingDto();
        dto.setSpråkkode(Språkkode.nn);
        dto.setBehandlingKøet(false);
        dto.setBehandlingPåVent(true);
        dto.setVenteårsak(Venteårsak.AVV_DOK);
        dto.setFristBehandlingPåVent(LocalDateTime.now().toString());

        var s = objectMapper.writeValueAsString(dto);
        assertThat(s)
            .contains("språkkode", "sprakkode")
            .contains("behandlingKøet", "behandlingKoet")
            .contains("venteÅrsakKode", "venteArsakKode")
            .contains("behandlingPåVent", "behandlingPaaVent")
            .contains("erPåVent", "erPaaVent")
            .contains("fristBehandlingPåVent", "fristBehandlingPaaVent");
    }

}
