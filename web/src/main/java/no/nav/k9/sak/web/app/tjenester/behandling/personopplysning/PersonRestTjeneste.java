package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.medlem.MedlemV2Dto;
import no.nav.k9.sak.kontrakt.person.PersonopplysningDto;
import no.nav.k9.sak.web.app.tjenester.behandling.medlem.MedlemDtoTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class PersonRestTjeneste {
    static final String BASE_PATH = "/behandling/person";

    public static final String MEDLEMSKAP_V2_PATH = BASE_PATH + "/medlemskap-v2";
    public static final String PERSONOPPLYSNINGER_PATH = BASE_PATH + "/personopplysninger";

    private MedlemDtoTjeneste medlemDtoTjeneste;
    private PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;

    public PersonRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public PersonRestTjeneste(MedlemDtoTjeneste medlemTjeneste,
                              PersonopplysningDtoTjeneste personopplysningTjeneste,
                              PersonopplysningDtoPersonIdentTjeneste personopplysningFnrFinder,
                              BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste) {
        this.medlemDtoTjeneste = medlemTjeneste;
        this.personopplysningDtoTjeneste = personopplysningTjeneste;
        this.personopplysningFnrFinder = personopplysningFnrFinder;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(PERSONOPPLYSNINGER_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public PersonopplysningDto getPersonopplysninger(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = getBehandlingsId(behandlingIdDto);
        Optional<PersonopplysningDto> personopplysningDto = personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId, LocalDate.now());
        if (personopplysningDto.isPresent()) {
            PersonopplysningDto pers = personopplysningDto.get();
            personopplysningFnrFinder.oppdaterMedPersonIdent(pers);
            return pers;
        } else {
            return null;
        }
    }

    @GET
    @Path(PERSONOPPLYSNINGER_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public PersonopplysningDto getPersonopplysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingsprosessApplikasjonTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        Long behandlingId = getBehandlingsId(new BehandlingIdDto(behandling.getId()));
        Optional<PersonopplysningDto> personopplysningDto = personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId, LocalDate.now());
        if (personopplysningDto.isPresent()) {
            PersonopplysningDto pers = personopplysningDto.get();
            personopplysningFnrFinder.oppdaterMedPersonIdent(pers);
            return pers;
        } else {
            return null;
        }
    }

    @GET
    @Path(MEDLEMSKAP_V2_PATH)
    @Operation(description = "Hent informasjon om medlemskap i Folketrygden for søker i behandling", tags = "behandling - person", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer Medlemskap, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MedlemV2Dto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public MedlemV2Dto hentMedlemskap(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingsprosessApplikasjonTjeneste.hentBehandling(behandlingUuid.getBehandlingUuid());
        Long behandlingId = getBehandlingsId(new BehandlingIdDto(behandling.getId()));
        Optional<MedlemV2Dto> medlemDto = medlemDtoTjeneste.lagMedlemPeriodisertDto(behandlingId);
        return medlemDto.orElse(null);
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
