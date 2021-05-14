package no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Map;

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
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.søknadsfrist.SøknadsfristTilstandDto;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class SøknadsfristRestTjeneste {

    private static final String PATH = "/behandling/søknadsfrist/status";
    public static final String SØKNADSFRIST_STATUS_PATH = PATH;

    private BehandlingRepository behandlingRepository;
    private SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;

    public SøknadsfristRestTjeneste() {
    }

    @Inject
    public SøknadsfristRestTjeneste(BehandlingRepository behandlingRepository,
                                    SøknadsfristTjenesteProvider søknadsfristTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
    }

    @GET
    @Operation(description = "Hent status på søknadsfrist", summary = ("Returnerer status for søknadsfrist."), tags = "søknadsfrist")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(SØKNADSFRIST_STATUS_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SøknadsfristTilstandDto utledStatusForKompletthet(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var referanse = BehandlingReferanse.fra(behandling);
        var vurderSøknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(referanse);

        Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> relevanteVurderteKravdokumentMedPeriodeForBehandling = vurderSøknadsfristTjeneste.relevanteVurderteKravdokumentMedPeriodeForBehandling(referanse);

        return new MapTilSøknadsfristDto().mapTil(relevanteVurderteKravdokumentMedPeriodeForBehandling);
    }

}
