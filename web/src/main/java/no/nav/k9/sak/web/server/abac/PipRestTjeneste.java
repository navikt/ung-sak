package no.nav.k9.sak.web.server.abac;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.PIP;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.pip.PipBehandlingsData;
import no.nav.foreldrepenger.behandlingslager.pip.PipRepository;
import no.nav.k9.sak.kontrakt.abac.PipDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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
    @BeskyttetRessurs(action = READ, ressurs = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Set<AktørId> hentAktørIdListeTilknyttetSak(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        Set<AktørId> aktører = pipRepository.hentAktørIdKnyttetTilSaksnummer(saksnummerDto.getVerdi());
        return aktører;
    }

    @GET
    @Path("/pipdata-for-behandling")
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(action = READ, ressurs = PIP)
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
