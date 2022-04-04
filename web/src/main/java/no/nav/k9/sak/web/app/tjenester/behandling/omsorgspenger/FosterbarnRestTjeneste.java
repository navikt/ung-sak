package no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Set;

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
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.person.FosterbarnDto;
import no.nav.k9.sak.kontrakt.person.FosterbarnListeDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;

@Path(FosterbarnRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Transactional
@ApplicationScoped
public class FosterbarnRestTjeneste {

    static final String BASE_PATH = "/behandling/fosterbarn";

    private FosterbarnRepository fosterbarnRepository;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;

    public FosterbarnRestTjeneste() {
        // for proxying
    }

    @Inject
    public FosterbarnRestTjeneste(BehandlingRepository behandlingRepository,
                                  FosterbarnRepository fosterbarnRepository,
                                  PersoninfoAdapter personinfoAdapter) {
        this.behandlingRepository = behandlingRepository;
        this.fosterbarnRepository = fosterbarnRepository;
        this.personinfoAdapter = personinfoAdapter;
    }

    /**
     * Hent fosterbarn registrert i k9
     */
    @GET
    @Path(BASE_PATH)
    @Operation(description = "Hent fosterbarn for omsorgspenger", tags = "behandling - fosterbarn", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Fosterbarn, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FosterbarnListeDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public FosterbarnListeDto hentFosterbarn(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var fosterbarnGrunnlag = fosterbarnRepository.hentHvisEksisterer(behandling.getId());
        var fosterbarn = fosterbarnGrunnlag.map(gr -> gr.getFosterbarna().getFosterbarn()).orElse(Set.of());

        var fosterbarnListe = fosterbarn
            .stream()
            .map(barn -> personinfoAdapter.hentPersoninfo(barn.getAktørId()))
            .map(personinfo -> new FosterbarnDto(personinfo.getPersonIdent().getIdent(), personinfo.getNavn(), personinfo.getFødselsdato()))
            .toList();
        return new FosterbarnListeDto(fosterbarnListe);
    }
}
