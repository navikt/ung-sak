package no.nav.k9.sak.web.app.tjenester.brev;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.dokument.bestill.DokumentBehandlingTjeneste;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.kontrakt.vedtak.VedtakVarselDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Transactional
public class BrevRestTjeneste {

    public static final String VARSEL_REVURDERING_PATH = "/brev/varsel/revurdering";
    public static final String HENT_VEDTAKVARSEL_PATH = "/brev/vedtak";
    public static final String BREV_BESTILL_PATH = "/brev/bestill";
    private static final Logger LOGGER = LoggerFactory.getLogger(BrevRestTjeneste.class);
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private VedtakVarselRepository vedtakVarselRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(VedtakVarselRepository vedtakVarselRepository,
                            BehandlingVedtakRepository behandlingVedtakRepository,
                            VilkårResultatRepository vilkårResultatRepository,
                            BehandlingRepository behandlingRepository,
                            DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
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
        oppdaterBehandlingBasertPåManueltBrev(DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getBehandlingId());
    }

    private void oppdaterBehandlingBasertPåManueltBrev(DokumentMalType brevmalkode, Long behandlingId) {
        if (DokumentMalType.REVURDERING_DOK.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_RESPONS_REVURDERING, behandlingId);
            registrerVarselOmRevurdering(behandlingId);
        } else if (DokumentMalType.FORLENGET_DOK.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManuelt(behandlingId);
        } else if (DokumentMalType.FORLENGET_MEDL_DOK.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManueltMedlemskap(behandlingId);
        }
    }

    private void registrerVarselOmRevurdering(Long behandlingId) {
        var varsel = vedtakVarselRepository.hentHvisEksisterer(behandlingId).orElse(new VedtakVarsel());
        varsel.setHarSendtVarselOmRevurdering(true);
        vedtakVarselRepository.lagre(behandlingId, varsel);
    }

    private void settBehandlingPåVent(Venteårsak avvResponsRevurdering, Long behandlingId) {
        dokumentBehandlingTjeneste.settBehandlingPåVent(behandlingId, avvResponsRevurdering);
    }

    @GET
    @Path(VARSEL_REVURDERING_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harSendtVarselOmRevurdering(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        return vedtakVarselRepository.hentHvisEksisterer(behandlingUuid.getBehandlingUuid()).orElse(new VedtakVarsel()).getErVarselOmRevurderingSendt(); // NOSONAR
    }

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

}
