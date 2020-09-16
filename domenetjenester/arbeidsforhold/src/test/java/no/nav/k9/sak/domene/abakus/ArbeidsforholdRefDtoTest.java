package no.nav.k9.sak.domene.abakus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.kodeverk.Fagsystem;

public class ArbeidsforholdRefDtoTest {

    private ObjectMapper om = new ObjectMapper();

    @Test
    public void skal_serialisere_dto() throws Exception {
        var dto = new ArbeidsforholdRefDto("a", "b", Fagsystem.AAREGISTERET);
        var json = om.writer().writeValueAsString(dto);

        System.out.println(json);

        var dto2 = om.reader().readValue(json, ArbeidsforholdRefDto.class);

        assertThat(dto2).isNotNull();
        assertThat(dto).isEqualTo(dto2);
    }


}
