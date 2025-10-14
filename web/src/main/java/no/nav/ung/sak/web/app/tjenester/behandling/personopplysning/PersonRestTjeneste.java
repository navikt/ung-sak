package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.person.PersonopplysningDto;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class PersonRestTjeneste {
    static final String BASE_PATH = "/behandling/person";

    public static final String PERSONOPPLYSNINGER_PATH = BASE_PATH + "/personopplysninger";

    private PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    public PersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PersonRestTjeneste(PersonopplysningDtoTjeneste personopplysningTjeneste,
                              PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder,
                              BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste) {
        this.personopplysningDtoTjeneste = personopplysningTjeneste;
        this.personopplysningFnrFinder = personopplysningFnrFinder;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
    }

    @GET
    @Path(PERSONOPPLYSNINGER_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker i behandling", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public PersonopplysningDto getPersonopplysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingsprosessApplikasjonTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        Long behandlingId = getBehandlingsId(new BehandlingIdDto(behandling.getId()));
        Optional<PersonopplysningDto> personopplysningDto = personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId);
        if (personopplysningDto.isPresent()) {
            PersonopplysningDto pers = personopplysningDto.get();
            personopplysningFnrFinder.oppdaterMedPersonIdent(pers);
            return pers;
        } else {
            return null;
        }
    }

    private Long getBehandlingsId(BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        if (behandlingId != null) {
            return behandlingId;
        } else {
            Behandling behandling = behandlingsprosessApplikasjonTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
            return behandling.getId();
        }
    }

}
