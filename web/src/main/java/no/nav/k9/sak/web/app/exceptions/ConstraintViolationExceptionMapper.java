package no.nav.k9.sak.web.app.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.hibernate.validator.internal.engine.path.PathImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeltFeilDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktKode;

public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger log = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        try {
            Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();

            Collection<FeltFeilDto> feilene = new ArrayList<>();
            for (ConstraintViolation<?> constraintViolation : constraintViolations) {
                String kode = getKode(constraintViolation.getLeafBean());
                String feltNavn = getFeltNavn(constraintViolation.getPropertyPath());
                feilene.add(new FeltFeilDto(feltNavn, constraintViolation.getMessage(), kode));
            }
            List<String> feltNavn = feilene.stream()
                .map(FeltFeilDto::getNavn)
                .toList();

            Feil feil = FeltValideringFeil.FACTORY.feltverdiKanIkkeValideres(feltNavn);
            feil.log(log);

            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new FeilDto(feil.getFeilmelding(), feilene))
                .type(MediaType.APPLICATION_JSON)
                .build();
        } finally {
            MDC.remove("prosess");
        }
    }

    private String getKode(Object leafBean) {
        return leafBean instanceof AksjonspunktKode ? ((AksjonspunktKode) leafBean).getKode() : null;
    }

    private String getFeltNavn(Path propertyPath) {
        return propertyPath instanceof PathImpl ? ((PathImpl) propertyPath).getLeafNode().toString() : null;
    }

}
