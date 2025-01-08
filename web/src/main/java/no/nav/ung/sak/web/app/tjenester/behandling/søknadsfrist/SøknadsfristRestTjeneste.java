package no.nav.ung.sak.web.app.tjenester.behandling.søknadsfrist;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
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
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.søknadsfrist.SøknadsfristTilstandDto;
import no.nav.ung.sak.behandlingslager.behandling.startdato.VurdertSøktPeriode;
import no.nav.ung.sak.søknadsfrist.KravDokument;
import no.nav.ung.sak.søknadsfrist.SøknadsfristTjenesteProvider;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

@ApplicationScoped
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class SøknadsfristRestTjeneste {

    private static final String PATH = "/behandling/søknadsfrist/status";
    public static final String SØKNADSFRIST_STATUS_PATH = PATH;

    private BehandlingRepository behandlingRepository;
    private SøknadsfristTjenesteProvider søknadsfristTjenesteProvider;
    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;

    public SøknadsfristRestTjeneste() {
    }

    @Inject
    public SøknadsfristRestTjeneste(BehandlingRepository behandlingRepository,
                                    SøknadsfristTjenesteProvider søknadsfristTjenesteProvider,
                                    AvklartSøknadsfristRepository avklartSøknadsfristRepository) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenesteProvider = søknadsfristTjenesteProvider;
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
    }

    @GET
    @Operation(description = "Hent status på søknadsfrist", summary = ("Returnerer status for søknadsfrist."), tags = "søknadsfrist")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @Path(SØKNADSFRIST_STATUS_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public SøknadsfristTilstandDto utledStatus(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        var referanse = BehandlingReferanse.fra(behandling);
        var vurderSøknadsfristTjeneste = søknadsfristTjenesteProvider.finnVurderSøknadsfristTjeneste(referanse);
        var avklartSøknadsfristResultat = avklartSøknadsfristRepository.hentHvisEksisterer(referanse.getBehandlingId());

        Map<KravDokument, List<VurdertSøktPeriode<VurdertSøktPeriode.SøktPeriodeData>>> relevanteVurderteKravdokumentMedPeriodeForBehandling = vurderSøknadsfristTjeneste.relevanteVurderteKravdokumentMedPeriodeForBehandling(referanse);

        return new MapTilSøknadsfristDto().mapTil(relevanteVurderteKravdokumentMedPeriodeForBehandling, avklartSøknadsfristResultat);
    }
}
