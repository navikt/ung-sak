package no.nav.ung.sak.web.app.tjenester.behandling.søknad;

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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.abac.AppAbacAttributtType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.søknad.HentSøknadPerioderDto;
import no.nav.ung.sak.kontrakt.søknad.SøknadDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.List;
import java.util.function.Function;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

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
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
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
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.FAGSAK)
    public List<Periode> hentSøknadPerioder(@Parameter(description = "Match kritierer for å lete opp fagsaker") @Valid @TilpassetAbacAttributt(supplierClass = MatchHentSøknadAttributter.class) HentSøknadPerioderDto hentSøknadPerioder) {
        return søknadDtoTjeneste.hentSøknadperioderPåFagsak(hentSøknadPerioder.getYtelseType(), hentSøknadPerioder.getBruker());
    }

    @POST
    @Path(SOKNAD_PERIODER_SAKSNUMMER_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter søknadsperioder med saksnummer", tags = "søknad", summary = ("Finner søknadspperioder med saksnummer"))
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.FAGSAK)
    public List<Periode> hentSøknadPerioderMedSaksnummer(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto idDto) {
        return søknadDtoTjeneste.hentSøknadperioderPåFagsak(idDto.getVerdi());
    }

    public static class MatchHentSøknadAttributter implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var m = (HentSøknadPerioderDto) obj;
            String fødselsnummer = m.getBruker().getIdent();
            String aktørId = m.getBruker().getAktørId();
            if (fødselsnummer == null && aktørId == null) {
                throw new IllegalArgumentException("Krever fødselsnummer eller aktørId her, mangler begge");
            }
            AbacDataAttributter abacAttributter = AbacDataAttributter.opprett();
            abacAttributter.leggTil(AppAbacAttributtType.YTELSETYPE, m.getYtelseType().getKode());
            if (fødselsnummer != null) {
                abacAttributter.leggTil(AppAbacAttributtType.SAKER_MED_FNR, fødselsnummer);
            } else {
                abacAttributter.leggTil(AppAbacAttributtType.SAKER_MED_AKTØR_ID, aktørId);
            }
            return abacAttributter;
        }
    }
}
