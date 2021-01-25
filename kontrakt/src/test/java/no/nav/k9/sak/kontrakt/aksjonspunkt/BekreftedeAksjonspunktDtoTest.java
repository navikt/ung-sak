package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.io.InputStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kontrakt.JsonUtil;
import no.nav.k9.sak.kontrakt.medlem.BekreftErMedlemVurderingDto;

public class BekreftedeAksjonspunktDtoTest {

    @Test
    public void sjekk_case_1() throws Exception {
        String jsonFile = "/aksjonspunkt/case-1.json";
        ObjectMapper mapper = JsonUtil.getObjectMapper();
        mapper.registerSubtypes(BekreftErMedlemVurderingDto.class);

        try (InputStream inputStream = this.getClass().getResourceAsStream(jsonFile)) {
            BekreftedeAksjonspunkterDto obj = mapper.readerFor(BekreftedeAksjonspunkterDto.class).readValue(inputStream);

            Assertions.assertThat(obj.getBehandlingVersjon()).isNotNull().isEqualTo(16);
        }
    }

}