package no.nav.k9.sak.web.app.exceptions;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.jpa.TomtResultatException;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingEndretKonfliktException;

public class KnownExceptionMappers {

    @SuppressWarnings("rawtypes")
    private final Map<Class<? extends Throwable>, ExceptionMapper> exceptionMappers = new LinkedHashMap<>();

    public KnownExceptionMappers() {

        // NB pass på rekkefølge dersom exceptions arver (håndter minst spesifikk til slutt)
        register(ConstraintViolationException.class, new ConstraintViolationExceptionMapper());
        register(JsonMappingException.class, new JsonMappingExceptionMapper());
        register(JsonParseException.class, new JsonParseExceptionMapper());
        register(ManglerTilgangException.class, new ManglerTilgangExceptionMapper());
        register(BehandlingEndretKonfliktException.class, new BehandlingEndretKonfliktExceptionMapper());
        register(TomtResultatException.class, new TomtResultatExceptionMapper());
        register(UnsupportedOperationException.class, new UnsupportedOperationExceptionMapper());
        register(VLException.class, new VLExceptionMapper());
        register(Throwable.class, new ThrowableExceptionMapper());
    }

    @SuppressWarnings("rawtypes")
    private void register(Class<? extends Throwable> exception, ExceptionMapper mapper) {
        exceptionMappers.put(exception, mapper);
    }

    @SuppressWarnings("rawtypes")
    public ExceptionMapper getMapper(Throwable exception) {
        for (var m : exceptionMappers.entrySet()) {
            if (m.getKey().isAssignableFrom(exception.getClass())) {
                return m.getValue();
            }
        }
        throw new UnsupportedOperationException("Skal aldri komme hit, mangler ExceptionMapper for:" + exception.getClass());
    }

    @SuppressWarnings("rawtypes")
    public Collection<ExceptionMapper> getExceptionMappers() {
        return exceptionMappers.values();
    }
}
