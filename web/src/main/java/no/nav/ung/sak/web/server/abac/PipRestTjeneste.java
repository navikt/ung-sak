package no.nav.ung.sak.web.server.abac;

import io.swagger.v3.oas.annotations.Operation;
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
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.kontrakt.abac.PipAktørerMedSporingslogghintDto;
import no.nav.ung.sak.kontrakt.abac.PipDtoV3;
import no.nav.ung.sak.kontrakt.abac.PipDtoV4;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.PIP;

@Path("/pip")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PipRestTjeneste {

    private Logger logger = LoggerFactory.getLogger(PipRestTjeneste.class);

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
        return pipRepository.hentAktørIdKnyttetTilFagsaker(Set.of(saksnummerDto.getVerdi()));
    }

    @GET
    @Path("/aktoer-for-sak-med-sporingslogghint")
    @Operation(description = "Henter aktørId'er tilknyttet en fagsak", tags = "pip")
    @BeskyttetRessurs(action = READ, resource = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public PipAktørerMedSporingslogghintDto hentAktørIdListeTilknyttetSakMedSporingslogghint(@NotNull @QueryParam("saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummerDto) {
        Set<AktørId> aktørerTilTilgangskontroll = pipRepository.hentAktørIdKnyttetTilFagsaker(Set.of(saksnummerDto.getVerdi()));
        Set<AktørId> aktørerTilSporingslogg = pipRepository.hentAktørIdForSporingslogg(Set.of(saksnummerDto.getVerdi()));
        return new PipAktørerMedSporingslogghintDto(aktørerTilTilgangskontroll, aktørerTilSporingslogg);
    }

    @GET
    @Path("/pipdata-for-behandling-v3")
    @Operation(description = "Henter saksnummer, aktørIder, fagsak- og behandlingstatus og ansvarlig saksbehandler tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(action = READ, resource = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Optional<PipDtoV3> hentPipDataTilknyttetBehandlingV3(@NotNull @QueryParam("behandlingUuid") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Optional<PipBehandlingsData> pipDataOpt = pipRepository.hentDataForBehandlingUuid(behandlingIdDto.getBehandlingUuid());
        if (pipDataOpt.isPresent()) {
            PipBehandlingsData pipData = pipDataOpt.get();
            Set<AktørId> personer = hentAktørIder(pipData);
            BehandlingStatus behandlingStatus = pipData.behandligStatus();
            FagsakStatus fagsakStatus = pipData.fagsakStatus();
            String ansvarligSaksbehandler = pipData.ansvarligSaksbehandler();
            Saksnummer saksnummer = pipData.saksnummer();
            return Optional.of(new PipDtoV3(saksnummer, personer, behandlingStatus, fagsakStatus, ansvarligSaksbehandler));
        } else {
            return Optional.empty();
        }
    }

    @GET
    @Path("/pipdata-for-behandling-v4")
    @Operation(description = "Henter saksnummer, aktørIder, fagsak- og behandlingstatus og ansvarlig saksbehandler tilknyttet til en behandling", tags = "pip")
    @BeskyttetRessurs(action = READ, resource = PIP)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Optional<PipDtoV4> hentPipDataTilknyttetBehandlingV4(@NotNull @QueryParam("behandlingUuid") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Optional<PipBehandlingsData> pipDataOpt = pipRepository.hentDataForBehandlingUuid(behandlingIdDto.getBehandlingUuid());
        if (pipDataOpt.isPresent()) {
            PipBehandlingsData pipData = pipDataOpt.get();
            Set<AktørId> personer = hentAktørIder(pipData);
            Set<AktørId> personerForSporingslogg = hentAktørIderForSporingslogg(pipData);
            BehandlingStatus behandlingStatus = pipData.behandligStatus();
            FagsakStatus fagsakStatus = pipData.fagsakStatus();
            String ansvarligSaksbehandler = pipData.ansvarligSaksbehandler();
            Saksnummer saksnummer = pipData.saksnummer();
            return Optional.of(new PipDtoV4(saksnummer, personer, personerForSporingslogg, behandlingStatus, fagsakStatus, ansvarligSaksbehandler));
        } else {
            return Optional.empty();
        }
    }

    private Set<AktørId> hentAktørIder(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdKnyttetTilFagsaker(List.of(pipBehandlingsData.saksnummer()));
    }

    private Set<AktørId> hentAktørIderForSporingslogg(PipBehandlingsData pipBehandlingsData) {
        return pipRepository.hentAktørIdForSporingslogg(pipBehandlingsData.saksnummer());
    }

}
