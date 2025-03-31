package no.nav.ung.sak.web.app.tjenester.etterlysning;

import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.etterlysning.EtterlysningDto;
import no.nav.ung.sak.kontrakt.etterlysning.UttalelseDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.List;
import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

@Path("")
@ApplicationScoped
@Transactional
public class EtterlysningRestTjeneste {

    public static final String PATH = "/behandling";
    public static final String ETTERLYSNINGER_PATH = PATH + "/etterlysninger";
    private EtterlysningRepository etterlysningRepository;
    private BehandlingRepository behandlingRepository;

    public EtterlysningRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public EtterlysningRestTjeneste(EtterlysningRepository etterlysningRepository, BehandlingRepository behandlingRepository) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(ETTERLYSNINGER_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<EtterlysningDto> hentEtterlysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var mappetEtterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId())
            .stream()
            .map(it -> new EtterlysningDto(
                it.getStatus(),
                it.getType(),
                new Periode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()),
                it.getEksternReferanse(),
                Optional.ofNullable(it.getUttalelse())
                    .map(u -> new UttalelseDto(u.getUttalelseBegrunnelse(), u.harGodtattEndringen())).orElse(null)))
            .toList();
        return mappetEtterlysninger;
    }

}
