package no.nav.foreldrepenger.web.app.tjenester.brev;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.List;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerApplikasjonTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.dto.BrevmalDto;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentProdusertDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
@Transactional
public class BrevRestTjeneste {

    public static final String MALER_PATH = "/brev/maler";
    public static final String DOKUMENT_SENDT_PATH = "/brev/dokument-sendt";
    public static final String VARSEL_REVURDERING_PATH = "/brev/varsel/revurdering";
    public static final String BREV_BESTILL_PATH = "/brev/bestill";
    private static final Logger LOGGER = LoggerFactory.getLogger(BrevRestTjeneste.class);
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private BehandlingRepository behandlingRepository;

    public BrevRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public BrevRestTjeneste(DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                            DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                            BehandlingRepository behandlingRepository) {
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path(BREV_BESTILL_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Bestiller generering og sending av brevet", tags = "brev")
    @BeskyttetRessurs(action = UPDATE, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public void bestillDokument(@Parameter(description = "Inneholder kode til brevmal og data som skal flettes inn i brevet") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BestillBrevDto bestillBrevDto) { // NOSONAR
        // FIXME: bør støttes behandlingUuid i fp-formidling
        LOGGER.info("Brev med brevmalkode={} bestilt på behandlingId={}", bestillBrevDto.getBrevmalkode(), bestillBrevDto.getBehandlingId());
        dokumentBestillerApplikasjonTjeneste.bestillDokument(bestillBrevDto, HistorikkAktør.SAKSBEHANDLER, true);
        oppdaterBehandlingBasertPåManueltBrev(DokumentMalType.fraKode(bestillBrevDto.getBrevmalkode()), bestillBrevDto.getBehandlingId());
    }

    private void oppdaterBehandlingBasertPåManueltBrev(DokumentMalType brevmalkode, Long behandlingId) {
        if (DokumentMalType.REVURDERING_DOK.equals(brevmalkode)) {
            settBehandlingPåVent(Venteårsak.AVV_RESPONS_REVURDERING, behandlingId);
        } else if (DokumentMalType.FORLENGET_DOK.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManuelt(behandlingId);
        } else if (DokumentMalType.FORLENGET_MEDL_DOK.equals(brevmalkode)) {
            dokumentBehandlingTjeneste.utvidBehandlingsfristManueltMedlemskap(behandlingId);
        }
    }

    private void settBehandlingPåVent(Venteårsak avvResponsRevurdering, Long behandlingId) {
        dokumentBehandlingTjeneste.settBehandlingPåVent(behandlingId, avvResponsRevurdering);
    }

    @GET
    @Path(MALER_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter liste over tilgjengelige brevtyper", tags = "brev")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<BrevmalDto> hentMaler(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.hentBrevmalerFor(behandling.getId());
    }

    @POST
    @Path(DOKUMENT_SENDT_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekker om dokument for mal er sendt", tags = "brev")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harProdusertDokument(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) DokumentProdusertDto dto) {
        Behandling behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.fraKode(dto.getDokumentMal())); // NOSONAR
    }

    @GET
    @Path(VARSEL_REVURDERING_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Sjekk har varsel sendt om revurdering", tags = "brev")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harSendtVarselOmRevurdering(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return dokumentBehandlingTjeneste.erDokumentProdusert(behandling.getId(), DokumentMalType.REVURDERING_DOK); // NOSONAR
    }
}
