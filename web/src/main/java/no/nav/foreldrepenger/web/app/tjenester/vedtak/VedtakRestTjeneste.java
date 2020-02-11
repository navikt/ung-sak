package no.nav.foreldrepenger.web.app.tjenester.vedtak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.vedtak.innsyn.VedtakInnsynTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("")
@ApplicationScoped
public class VedtakRestTjeneste {

    public static final String HENT_VEDTAKSDOKUMENT_PATH = "/vedtak/hent-vedtaksdokument";

    private VedtakInnsynTjeneste vedtakInnsynTjeneste;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;

    public VedtakRestTjeneste() {
        // for resteasy
    }

    @Inject
    public VedtakRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                              VedtakInnsynTjeneste vedtakInnsynTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.vedtakInnsynTjeneste = vedtakInnsynTjeneste;
    }

    @GET
    @Path(HENT_VEDTAKSDOKUMENT_PATH)
    @Operation(description = "Hent vedtaksdokument gitt behandlingId", tags = "vedtak")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentVedtaksdokument(@NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId for vedtaksdokument") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        String resultat = vedtakInnsynTjeneste.hentVedtaksdokument(behandling.getId());
        return Response.ok(resultat, "text/html").build();
    }

    @GET
    @Path(HENT_VEDTAKSDOKUMENT_PATH)
    @Operation(description = "Hent vedtaksdokument gitt behandlingId", summary = ("Returnerer vedtaksdokument som er tilknyttet behadnlingId."), tags = "vedtak")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentVedtaksdokument(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto uuidDto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());
        String resultat = vedtakInnsynTjeneste.hentVedtaksdokument(behandling.getId());
        return Response.ok(resultat, "text/html").build();
    }

}
