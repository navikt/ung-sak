package no.nav.ung.sak.web.app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

import no.nav.ung.sak.kontrakt.FeilDto;

public class JsonPdpRequestMapperExceptionMapperTest {

    @Test
    public void skal_mappe_InvalidTypeIdException() throws Exception {
        JsonMappingExceptionMapper mapper = new JsonMappingExceptionMapper();
        @SuppressWarnings("resource")
        Response resultat = mapper.toResponse(new InvalidTypeIdException(null, "Ukjent type-kode", null, "23525"));
        FeilDto dto = (FeilDto) resultat.getEntity();
        assertThat(dto.getFeilmelding()).isEqualTo("JSON-mapping feil: Ukjent type-kode");
        assertThat(dto.getFeltFeil()).isNull();
    }
}
