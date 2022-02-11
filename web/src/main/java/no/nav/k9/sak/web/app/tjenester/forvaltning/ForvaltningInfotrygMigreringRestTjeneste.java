package no.nav.k9.sak.web.app.tjenester.forvaltning;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;

@ApplicationScoped
@Transactional
@Path("/infotrygdmigrering")
public class ForvaltningInfotrygMigreringRestTjeneste {

    private FagsakRepository fagsakRepository;

    public ForvaltningInfotrygMigreringRestTjeneste() {
    }

    @Inject
    public ForvaltningInfotrygMigreringRestTjeneste(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    @GET
    @Path("/skjæringstidspunkter")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Hent skjæringstidspunkter for infotrygdmigrering for saker", tags = "infotrygdmigrering")
    @BeskyttetRessurs(action = READ, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getSkjæringstidspunkter(@QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) SaksnummerDto saksnummerDto) { // NOSONAR
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummerDto.getVerdi());
        var infotrygdMigreringer = fagsak.map(Fagsak::getId)
            .stream()
            .flatMap(id -> fagsakRepository.hentAlleSakInfotrygdMigreringer(id).stream())
            .map(migrering -> new MigrertSkjæringstidspunktDto(migrering.getSkjæringstidspunkt(), migrering.getAktiv()))
            .collect(Collectors.toList());
        return Response.ok(infotrygdMigreringer).build();
    }

}
