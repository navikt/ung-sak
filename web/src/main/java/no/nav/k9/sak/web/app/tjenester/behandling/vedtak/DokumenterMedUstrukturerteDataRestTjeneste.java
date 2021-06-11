package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.k9.felles.integrasjon.saf.DokumentInfoResponseProjection;
import no.nav.k9.felles.integrasjon.saf.JournalpostQueryRequest;
import no.nav.k9.felles.integrasjon.saf.JournalpostResponseProjection;
import no.nav.k9.felles.integrasjon.saf.SafTjeneste;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentIdDto;
import no.nav.k9.sak.kontrakt.vedtak.DokumentMedUstrukturerteDataDto;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument.SykdomDokumentRestTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentRepository;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class DokumenterMedUstrukturerteDataRestTjeneste {

    public static final String FRITEKSTDOKUMENTER_PATH = "/behandling/vedtak/fritekstdokumenter";

    private BehandlingRepository behandlingRepository;
    private SykdomDokumentRepository sykdomDokumentRepository;
    private SafTjeneste safTjeneste;

    public DokumenterMedUstrukturerteDataRestTjeneste() {
        //
    }

    @Inject
    public DokumenterMedUstrukturerteDataRestTjeneste(BehandlingRepository behandlingRepository, SykdomDokumentRepository sykdomDokumentRepository, SafTjeneste safTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.sykdomDokumentRepository = sykdomDokumentRepository;
        this.safTjeneste = safTjeneste;
    }

    @GET
    @Path(FRITEKSTDOKUMENTER_PATH)
    @Operation(description = "Hent liste over dokumenter flagget med at de inneholder informasjon som ikke er/kan bli punsjet"
        , tags = "vedtak"
        , responses = {
            @ApiResponse(responseCode = "200",
                content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = DokumentMedUstrukturerteDataDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    public List<DokumentMedUstrukturerteDataDto> hentDokumenterMedUstrukturerteData(
            @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
            BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());

        if (behandling.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            return Collections.emptyList();
        }

        List<SykdomDokument> sykdomDokumenter = sykdomDokumentRepository.hentNyeDokumenterFor(behandlingUuid.getBehandlingUuid());

        return sykdomDokumenter.stream()
            .filter(SykdomDokument::isHarInfoSomIkkeKanPunsjes)
            .map(d -> new DokumentMedUstrukturerteDataDto(
                "" + d.getId(),
                hentTittel(d.getJournalpostId()),
                d.getType(),
                d.getDatert(),
                Arrays.asList(linkForGetDokumentinnhold(behandlingUuid.getBehandlingUuid().toString(), "" + d.getId()))))
            .collect(Collectors.toList());
    }

    private String hentTittel(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());
        var projection = new JournalpostResponseProjection()
            .kanal()
            .journalposttype()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel());
        var journalpost = safTjeneste.hentJournalpostInfo(query, projection);

        return journalpost.getTittel();
    }

    private ResourceLink linkForGetDokumentinnhold(String behandlingUuid, String sykdomDokumentId) {
        return ResourceLink.get(BehandlingDtoUtil.getApiPath(SykdomDokumentRestTjeneste.DOKUMENT_INNHOLD_PATH), "sykdom-dokument-innhold", Map.of(BehandlingUuidDto.NAME, behandlingUuid, SykdomDokumentIdDto.NAME, sykdomDokumentId));
    }
}
