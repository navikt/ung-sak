package no.nav.k9.sak.web.server.abac;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.k9.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.k9.sak.behandlingslager.pip.PipRepository;
import no.nav.k9.sak.kontrakt.abac.PipDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.k9.abac.BeskyttetRessursKoder.PIP;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

@Path("/pip")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PipRestTjeneste {

    private PipRepository pipRepository;

    @Inject
    public PipRestTjeneste(PipRepository pipRepository) {
        this.pipRepository = pipRepository;
    }

    public PipRestTjeneste() {
        // Ja gjett tre ganger på hva denne er til for.
    }

    @GET
    @Path("/aktoer-for-sak")
    @Operation(description = "Henter aktørId'er tilknyttet en fagsak", tags = "pip")
    @BeskyttetRessurs(action = READ, resource = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Set<AktørId> hentAktørIdListeTilknyttetSak(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        Set<AktørId> aktører = pipRepository.hentAktørIdKnyttetTilSaksnummer(saksnummerDto.getVerdi());
        return aktører;
    }

    @GET
    @Path("/pipdata-for-behandling")
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(action = READ, resource = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public PipDto hentAktørIdListeTilknyttetBehandling(@NotNull @QueryParam("behandlingUuid") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Optional<PipBehandlingsData> pipData = pipRepository.hentDataForBehandlingUuid(behandlingIdDto.getBehandlingUuid());
        PipDto pipDto = new PipDto();
        pipData.ifPresent(pip -> {
            pipDto.setAktørIder(hentAktørIder(pip));
            pipDto.setBehandlingStatus(AbacUtil.oversettBehandlingStatus(pip.getBehandligStatus()).map(AbacBehandlingStatus::getEksternKode).orElse(null));
            pipDto.setFagsakStatus(AbacUtil.oversettFagstatus(pip.getFagsakStatus()).map(AbacFagsakStatus::getEksternKode).orElse(null));
        });
        return pipDto;
    }

    private Set<AktørId> hentAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.getFagsakId()));
    }

}
