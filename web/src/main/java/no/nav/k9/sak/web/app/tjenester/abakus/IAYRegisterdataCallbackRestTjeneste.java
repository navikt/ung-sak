package no.nav.k9.sak.web.app.tjenester.abakus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.abakus.callback.registerdata.CallbackDto;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Objects;
import java.util.function.Function;

import static no.nav.abakus.callback.registerdata.Grunnlag.IAY;
import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

@Path("/registerdata")
@ApplicationScoped
@Transactional
public class IAYRegisterdataCallbackRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(IAYRegisterdataCallbackRestTjeneste.class);

    private IAYRegisterdataTjeneste iayTjeneste;
    private BehandlingLåsRepository låsRepository;

    public IAYRegisterdataCallbackRestTjeneste() {
    }

    @Inject
    public IAYRegisterdataCallbackRestTjeneste(IAYRegisterdataTjeneste iayTjeneste,
                                               BehandlingLåsRepository låsRepository) {
        this.iayTjeneste = iayTjeneste;
        this.låsRepository = låsRepository;
    }

    @POST
    @Path("/iay/callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Callback når registerinnhenting av IAY har blitt fullført i Abakus", tags = "registerdata")
    @BeskyttetRessurs(action = UPDATE, resource = APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response callback(@Parameter(description = "callbackDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) CallbackDto dto) {
        if (Objects.equals(IAY, dto.getGrunnlagType())) {
            // Ta lås
            BehandlingLås behandlingLås = låsRepository.taLås(dto.getAvsenderRef().getReferanse());
            // Oppdaterer grunnlag med ny referanse
            RegisterdataCallback registerdataCallback = new RegisterdataCallback(behandlingLås.getBehandlingId(),
                dto.getOpprinneligGrunnlagRef() != null ? dto.getOpprinneligGrunnlagRef().getReferanse() : null,
                dto.getOppdatertGrunnlagRef().getReferanse(),
                dto.getOpprettetTidspunkt());

            iayTjeneste.håndterCallback(registerdataCallback);
        } else {
            log.info("Mottatt registerdata callback på IAY-endepunkt for grunnlag av {}", dto);
        }

        return Response.accepted().build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
