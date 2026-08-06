package no.nav.ung.sak.web.app.tjenester.forvaltning;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/forvaltning/behandling")
@ApplicationScoped
@Transactional
public class ForvaltningBehandlingRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final Logger log = LoggerFactory.getLogger(ForvaltningBehandlingRestTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    public ForvaltningBehandlingRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningBehandlingRestTjeneste(BehandlingRepository behandlingRepository,
                                             HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;
    }

    @POST
    @Path("/{behandlingId}/henlegg")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(
        summary = "Henlegger en åpen behandling",
        description = """
            Henlegger en åpen behandling med angitt årsak.

            Beregnet for bruk av driftsrollen når det er nødvendig å lukke behandlinger \
            som ikke kan eller bør ferdigbehandles på vanlig måte — for eksempel \
            behandlinger som ble opprettet som følge av en feilaktig eller duplikat søknad, \
            og der saksbehandlerrollen ikke er tilgjengelig for å gjøre dette via vanlig GUI.

            Endepunktet utfører følgende valideringer før henleggelse:
            - 404 dersom behandlingen ikke finnes
            - 409 dersom behandlingen allerede er avsluttet eller er under iverksettelse av vedtak
            - 400 dersom årsakkoden ikke er en gyldig henleggelseskode for søknader
            """,
        tags = "forvaltning"
    )
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response henleggBehandling(
        @Parameter(description = "Behandlingens numeriske ID") @PathParam("behandlingId") @NotNull Long behandlingId,
        @Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) HenleggForvaltningRequest request
    ) {
        var behandlingOpt = behandlingRepository.hentBehandlingHvisFinnes(behandlingId);
        if (behandlingOpt.isEmpty()) {
            return feil(Response.Status.NOT_FOUND, "Behandling ikke funnet: " + behandlingId);
        }

        var behandling = behandlingOpt.get();
        if (behandling.getStatus().erFerdigbehandletStatus()) {
            return feil(Response.Status.CONFLICT,
                "Behandling %d kan ikke henlegges fordi den har status: %s (%s)."
                    .formatted(behandlingId, behandling.getStatus().getKode(), behandling.getStatus().getNavn()));
        }

        var årsak = request.årsak();
        if (!årsak.isBehandlingsresultatHenlagt()) {
            return feil(Response.Status.BAD_REQUEST,
                "Ugyldig henleggelsesårsak: '%s' er ikke en gyldig henleggelseskode for søknader.".formatted(årsak.getKode()));
        }

        henleggBehandlingTjeneste.henleggBehandlingAvSaksbehandler(
            String.valueOf(behandlingId),
            årsak,
            request.begrunnelse()
        );

        log.info("Henla behandling {} med årsak {}", behandlingId, request.årsak().getKode());
        return Response.ok().build();
    }

    private Response feil(Response.Status status, String melding) {
        log.warn(melding);
        return Response.status(status).entity(melding).build();
    }

    public record HenleggForvaltningRequest(
        @JsonProperty(value = "årsak", required = true)
        @NotNull
        @Schema(
            description = "Henleggelsesårsak. Kun gyldige henleggelseskoder er tillatt.",
            allowableValues = {
                "HENLAGT_FEILOPPRETTET",
                "HENLAGT_SØKNAD_TRUKKET",
                "HENLAGT_BRUKER_DØD",
                "HENLAGT_MASKINELT",
                "HENLAGT_SØKNAD_MANGLER",
                "MANGLER_BEREGNINGSREGLER"
            }
        )
        BehandlingResultatType årsak,

        @JsonProperty(value = "begrunnelse", required = true)
        @NotNull
        @Size(max = 4000)
        @Pattern(regexp = Patterns.FRITEKST, message = Patterns.FRITEKST_MISMATCH_MELDING)
        String begrunnelse
    ) {}
}
