package no.nav.ung.sak.kontrakt.aksjonspunkt;

import java.io.InputStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.ung.kontrakt.JsonUtil;
import no.nav.ung.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;

public class BekreftedeAksjonspunktDtoTest {

    @Test
    public void sjekk_case_1() throws Exception {
        String jsonFile = "/aksjonspunkt/case-1.json";
        ObjectMapper mapper = JsonUtil.getObjectMapper();
        mapper.registerSubtypes(ForeslaVedtakAksjonspunktDto.class);

        try (InputStream inputStream = this.getClass().getResourceAsStream(jsonFile)) {
            BekreftedeAksjonspunkterDto obj = mapper.readerFor(BekreftedeAksjonspunkterDto.class).readValue(inputStream);

            Assertions.assertThat(obj.getBehandlingVersjon()).isNotNull().isEqualTo(16);
        }
    }

}
