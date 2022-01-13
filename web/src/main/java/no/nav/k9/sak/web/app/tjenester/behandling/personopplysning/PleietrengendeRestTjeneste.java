package no.nav.k9.sak.web.app.tjenester.behandling.personopplysning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.person.PersonopplysningDto;
import no.nav.k9.sak.kontrakt.person.PersonopplysningPleietrengendeDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;

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

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class PleietrengendeRestTjeneste {

    public static final String BASE_PATH = "/behandling/pleietrengende";

    private BehandlingRepository behandlingRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private TpsTjeneste tpsTjeneste;
    private SykdomDokumentRepository sykdomDokumentRepository;

    public PleietrengendeRestTjeneste() {
        // CDI
    }

    @Inject
    public PleietrengendeRestTjeneste(BehandlingRepository behandlingRepository,
                                      PersonopplysningTjeneste personopplysningTjeneste,
                                      TpsTjeneste tpsTjeneste,
                                      SykdomDokumentRepository sykdomDokumentRepository) {
        this.behandlingRepository = behandlingRepository;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.tpsTjeneste = tpsTjeneste;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
    }

    @GET
    @Path(BASE_PATH)
    @Operation(description = "Hent informasjon om personopplysninger søker i behandling", tags = "behandling - person", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Personopplysninger, null hvis ikke finnes (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PersonopplysningDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public PersonopplysningPleietrengendeDto getPersonopplysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var ref = BehandlingReferanse.fra(behandling);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref, ref.getFagsakPeriode().getFomDato());
        var pleietrengendePersonopplysninger = personopplysningerAggregat.getPersonopplysning(behandling.getFagsak().getPleietrengendeAktørId());
        var personopplysningPleietrengendeDto = new PersonopplysningPleietrengendeDto();
        personopplysningPleietrengendeDto.setNavn(pleietrengendePersonopplysninger.getNavn());
        personopplysningPleietrengendeDto.setDodsdato(pleietrengendePersonopplysninger.getDødsdato());
        var fnr = tpsTjeneste.hentFnr(pleietrengendePersonopplysninger.getAktørId());
        if (fnr.isPresent()) {
            personopplysningPleietrengendeDto.setFnr(fnr.get().getIdent());
        }
        var sykdomDiagnosekoder = sykdomDokumentRepository.hentDiagnosekoder(behandling.getFagsak().getPleietrengendeAktørId());
        var diagnosekoder = sykdomDiagnosekoder.getDiagnosekoder().stream().map(diagnosekode -> diagnosekode.getDiagnosekode()).toList();
        personopplysningPleietrengendeDto.setDiagnosekoder(diagnosekoder);
        return personopplysningPleietrengendeDto;
    }

}
