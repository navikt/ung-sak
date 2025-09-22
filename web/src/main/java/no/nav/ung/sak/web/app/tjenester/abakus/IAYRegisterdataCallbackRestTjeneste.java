package no.nav.ung.sak.web.app.tjenester.abakus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.abakus.callback.registerdata.CallbackDto;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.api.PollTaskAfterTransaction;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.domene.arbeidsforhold.RegisterdataCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;

import static no.nav.abakus.callback.registerdata.Grunnlag.IAY;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.UPDATE;

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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Callback når registerinnhenting av IAY har blitt fullført i Abakus", tags = "registerdata")
    @PollTaskAfterTransaction
    @BeskyttetRessurs(action = UPDATE, resource = BeskyttetRessursResourceType.APPLIKASJON)
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
