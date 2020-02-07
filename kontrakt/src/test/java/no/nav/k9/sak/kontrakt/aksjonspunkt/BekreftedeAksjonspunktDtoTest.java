package no.nav.k9.sak.kontrakt.aksjonspunkt;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kontrakt.JsonUtil;
import no.nav.k9.sak.kontrakt.medlem.BekreftErMedlemVurderingDto;

public class BekreftedeAksjonspunktDtoTest {

    @Test
    public void sjekk_case_1() throws Exception {
        String jsonFile = "/aksjonspunkt/case-1.json";
        ObjectMapper mapper = JsonUtil.getObjectMapper();
        mapper.registerSubtypes(BekreftErMedlemVurderingDto.class);

        BekreftedeAksjonspunkterDto obj = mapper.readerFor(BekreftedeAksjonspunkterDto.class).readValue(this.getClass().getResourceAsStream(jsonFile));

        Assertions.assertThat(obj.getBehandlingVersjon()).isNotNull().isEqualTo(16);
    }

}