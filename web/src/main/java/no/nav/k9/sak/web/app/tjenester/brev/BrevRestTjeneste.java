package no.nav.k9.sak.web.app.tjenester.brev;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import no.nav.k9.felles.integrasjon.organisasjon.OrganisasjonRestKlient;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.vedtak.VedtakVarselDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
@Transactional
public class BrevRestTjeneste {

    public static final String HENT_VEDTAKVARSEL_PATH = "/brev/vedtak";
    public static final String BREV_BESTILL_PATH = "/brev/bestill";
    public static final String EREG_OPPSLAG_PATH = "/brev/mottaker-info/ereg";
    private static final Logger LOGGER = LoggerFactory.getLogger(BrevRestTjeneste.class);
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private VedtakVarselRepository vedtakVarselRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OrganisasjonRestKlient eregRestKlient;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(VedtakVarselRepository vedtakVarselRepository,
                            BehandlingVedtakRepository behandlingVedtakRepository,
                            VilkårResultatRepository vilkårResultatRepository,
                            BehandlingRepository behandlingRepository,
                            DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                            OrganisasjonRestKlient eregRestKlient) {
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.eregRestKlient = eregRestKlient;
    }

    @POST
    @Path(BREV_BESTILL_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void bestillDokument(@Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BestillBrevDto bestillBrevDto) { // NOSONAR
        // FIXME: bør støttes behandlingUuid i formidling
        LOGGER.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.getBrevmalkode(), bestillBrevDto.getBehandlingId());
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER);
    }

    /** @deprecated brukes bare av FRISINN (per 2021-03-22). */
    @Deprecated(forRemoval = true)
    @GET
    @Path(HENT_VEDTAKVARSEL_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent vedtak varsel gitt behandlingId", tags = "vedtak")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentVedtakVarsel(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var varsel = lagVedtakVarsel(behandlingUuid.getBehandlingUuid());
        if (varsel.isEmpty()) {
            return Response.noContent().build();
        } else {
            return Response.ok(varsel.get(), MediaType.APPLICATION_JSON).build();
        }
    }


    private Optional<VedtakVarselDto> lagVedtakVarsel(UUID behandlingUuid) {
        var vedtakVarsel = vedtakVarselRepository.hentHvisEksisterer(behandlingUuid).orElse(null);
        if (vedtakVarsel == null) {
            return Optional.empty();
        }
        var dto = new VedtakVarselDto();

        // brev data
        dto.setAvslagsarsakFritekst(vedtakVarsel.getAvslagarsakFritekst());
        dto.setVedtaksbrev(vedtakVarsel.getVedtaksbrev());
        dto.setOverskrift(vedtakVarsel.getOverskrift());
        dto.setFritekstbrev(vedtakVarsel.getFritekstbrev());
        dto.setRedusertUtbetalingÅrsaker(vedtakVarsel.getRedusertUtbetalingÅrsaker());

        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakFor(behandlingUuid);
        behandlingVedtak.ifPresent(v -> dto.setVedtaksdato(v.getVedtaksdato()));
        var behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid);
        behandling.ifPresent(it -> leggPåAvslagsÅrsaker(it, dto));

        return Optional.of(dto);
    }

    private void leggPåAvslagsÅrsaker(Behandling behandling, VedtakVarselDto dto) {
        var vilkårResultat = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        vilkårResultat.ifPresent(it -> setAvslagsÅrsaker(dto, it));
    }

    private void setAvslagsÅrsaker(VedtakVarselDto dto, Vilkårene it) {
        var vilkårMedAvslagsårsaker = it.getVilkårMedAvslagsårsaker();
        if (vilkårMedAvslagsårsaker.isEmpty()) {
            return;
        }
        dto.setAvslagsarsaker(vilkårMedAvslagsårsaker);
        dto.setAvslagsarsak(vilkårMedAvslagsårsaker.values().stream().flatMap(Collection::stream).findFirst().orElse(null));
    }

    /**
     * Gjere oppslag i intern ereg service (tilsvarer enhetsregisteret i Brønnøysund) og svarer tilbake ønska info om
     * gitt organisasjon. For frontend-oppslag i forbindelse med sending av brev til tredjepart.
     */
    @POST
    @Path(EREG_OPPSLAG_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Hent navnet til gitt organisasjonsnr for sending til tredjepart", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON)
    public Response getOrganisasjon(@NotNull @Valid @TilpassetAbacAttributt(supplierClass = IngenTilgangsAttributter.class) OrganisasjonsnrDto organisasjonsnrDto) {
        var orgnr = organisasjonsnrDto.getOrgnr();
        if (orgnr.length() != 9 || !orgnr.matches("\\d{9}")) {
            return Response.status(400, "organisasjonsnrDto must be 9 digits").build();
        }
        try {
            var org = eregRestKlient.hentOrganisasjon(orgnr);
            final var response = new BrevMottakerinfoEregResponseDto(org.getNavn());
            return Response.ok(response).build();
        } catch (IllegalArgumentException e) {
            // If org number is not found, it seems like eregRestKlient throws IllegalArgumentException
            // with the message matched below. At least this happens in dev.
            // For this endpoint I want to return empty json in the case when given organization
            // number is not found, so returns that below then.
            if (e.getMessage().equalsIgnoreCase("argument \"content\" is null")) {
                final var emptyObject = new Object();
                return Response.ok(emptyObject).build();
            }
            // If some other IllegalArgumentException happens, rethrow it.
            throw e;
        }
    }

    public static class IngenTilgangsAttributter implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
