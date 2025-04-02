package no.nav.ung.sak.web.server.abac;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.kontrakt.abac.PipDto;
import no.nav.ung.sak.kontrakt.abac.PipDtoV2;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.AktørId;
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

import static no.nav.ung.abac.BeskyttetRessursKoder.PIP;
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

    @GET
    @Path("/pipdata-for-behandling-v2")
    @Operation(description = "Henter aktørIder, fagsak- og behandlingstatus og ansvarlig saksbehandler tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(action = READ, resource = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Optional<PipDtoV2> hentPipDataTilknyttetBehandlingV2(@NotNull @QueryParam("behandlingUuid") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Optional<PipBehandlingsData> pipDataOpt = pipRepository.hentDataForBehandlingUuid(behandlingIdDto.getBehandlingUuid());
        if (pipDataOpt.isPresent()) {
            PipBehandlingsData pipData = pipDataOpt.get();
            Set<AktørId> personer = hentAktørIder(pipData);
            BehandlingStatus behandlingStatus = BehandlingStatus.fraKode(pipData.getBehandligStatus());
            FagsakStatus fagsakStatus = FagsakStatus.fraKode(pipData.getFagsakStatus());
            String ansvarligSaksbehandler = pipData.getAnsvarligSaksbehandler().orElse(null);
            return Optional.of(new PipDtoV2(personer, behandlingStatus, fagsakStatus, ansvarligSaksbehandler));
        } else {
            return Optional.empty();
        }
    }

    private Set<AktørId> hentAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.getFagsakId()));
    }

}
