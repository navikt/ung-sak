package no.nav.ung.sak.web.app.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.ung.sak.kontrakt.FeilDto;

public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger log = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        Feil feil = JsonMappingFeil.FACTORY.jsonParseFeil(exception.getMessage(), exception);
        feil.log(log);
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new FeilDto(feil.getFeilmelding()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    interface JsonMappingFeil extends DeklarerteFeil {

        JsonMappingFeil FACTORY = FeilFactory.create(JsonMappingFeil.class);

        @TekniskFeil(feilkode = "FP-299955", feilmelding = "JSON-parsing feil: %s", logLevel = LogLevel.WARN)
        Feil jsonParseFeil(String feilmelding, JsonParseException e);
    }

}
