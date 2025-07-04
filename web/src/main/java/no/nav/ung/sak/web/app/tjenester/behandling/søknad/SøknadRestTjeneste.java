package no.nav.ung.sak.web.app.tjenester.behandling.søknad;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.søknad.HentSøknadPerioderDto;
import no.nav.ung.sak.kontrakt.søknad.SøknadDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

@Produces(MediaType.APPLICATION_JSON)
@Path("")
@ApplicationScoped
@Transactional
public class SøknadRestTjeneste {

    public static final String SOKNAD_PATH = "/behandling/soknad";
    public static final String SOKNAD_PERIODER_PATH = "/behandling/soknad/perioder";
    public static final String SOKNAD_PERIODER_SAKSNUMMER_PATH = "/behandling/soknad/perioder/saksnummer";

    private BehandlingRepository behandlingRepository;
    private SøknadDtoTjeneste søknadDtoTjeneste;

    public SøknadRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider, SøknadDtoTjeneste søknadDtoTjeneste) {
        this.søknadDtoTjeneste = søknadDtoTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();

    }

    @GET
    @Path(SOKNAD_PATH)
    @Operation(description = "Hent informasjon om søknad", tags = "søknad", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer Søknad, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SøknadDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SøknadDto getSøknad(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        return søknadDtoTjeneste.mapFra(behandling).orElse(null);
    }

    @POST
    @Path(SOKNAD_PERIODER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter søknadsperioder på siste fagsak for deltaker", tags = "søknad", summary = ("Finner søknadspperioder på siste fagsak"))
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public List<Periode> hentSøknadPerioder(@Parameter(description = "Match kritierer for å lete opp fagsaker") @Valid @TilpassetAbacAttributt(supplierClass = MatchHentSøknadAttributter.class) HentSøknadPerioderDto hentSøknadPerioder) {
        return søknadDtoTjeneste.hentSøknadperioderPåFagsak(hentSøknadPerioder.getYtelseType(), hentSøknadPerioder.getBruker());
    }

    @POST
    @Path(SOKNAD_PERIODER_SAKSNUMMER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter søknadsperioder med saksnummer", tags = "søknad", summary = ("Finner søknadspperioder med saksnummer"))
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public List<Periode> hentSøknadPerioderMedSaksnummer(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto idDto) {
        return søknadDtoTjeneste.hentSøknadperioderPåFagsak(idDto.getVerdi());
    }

    public static class MatchHentSøknadAttributter implements Function<Object, AbacDataAttributter> {
        private static final StandardAbacAttributtType AKTØR_ID_TYPE = StandardAbacAttributtType.AKTØR_ID;
        private static final StandardAbacAttributtType FNR_TYPE = StandardAbacAttributtType.FNR;

        @Override
        public AbacDataAttributter apply(Object obj) {
            var m = (HentSøknadPerioderDto) obj;
            var abac = AbacDataAttributter.opprett();

            Optional.ofNullable(m.getBruker()).map(PersonIdent::getIdent).ifPresent(v -> abac.leggTil(FNR_TYPE, v));
            Optional.ofNullable(m.getBruker()).map(PersonIdent::getAktørId).ifPresent(v -> abac.leggTil(AKTØR_ID_TYPE, v));

            // må ha minst en aktørid
            if (abac.getVerdier(FNR_TYPE).isEmpty() && abac.getVerdier(AKTØR_ID_TYPE).isEmpty()) {
                throw new IllegalArgumentException("Må ha minst en aktørid eller fnr oppgitt");
            }
            return abac;
        }
    }
}
