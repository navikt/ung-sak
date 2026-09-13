package no.nav.ung.sak.web.app.tjenester.forvaltning;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.abac.StandardAbacAttributt;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Path("/forvaltning/ungdomsprogram")
@ApplicationScoped
@Transactional
public class ForvaltningUngdomsprogramRegisterRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final Logger log = LoggerFactory.getLogger(ForvaltningUngdomsprogramRegisterRestTjeneste.class);

    private FagsakRepository fagsakRepository;
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;
    private EntityManager entityManager;

    public ForvaltningUngdomsprogramRegisterRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public ForvaltningUngdomsprogramRegisterRestTjeneste(FagsakRepository fagsakRepository,
                                                          UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient,
                                                          EntityManager entityManager) {
        this.fagsakRepository = fagsakRepository;
        this.ungdomsprogramRegisterKlient = ungdomsprogramRegisterKlient;
        this.entityManager = entityManager;
    }

    @POST
    @Path("/marker-deltakelse-sokt")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Marker deltakelsen tilhørende saken som søkt i ung-deltakelse-opplyser. " +
        "Brukes til å rette opp saker der papirsøknad er journalført uten at deltakelsen ble markert søkt, " +
        "f.eks. pga. driftsavbrudd eller saker journalført før dette ble håndtert automatisk. " +
        "NB: Ikke idempotent på tidspunkt - hvert kall setter søkt-tidspunktet til nå, selv om deltakelsen allerede er markert søkt.",
        summary = "Marker deltakelse som søkt", tags = "forvaltning")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.UPDATE, resource = BeskyttetRessursResourceType.DRIFT)
    public Response markerDeltakelseSomSøkt(@Valid @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) MarkerDeltakelseSøktRequest dto) {
        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(dto.saksnummer());
        if (fagsakOpt.isEmpty()) {
            return feil(Response.Status.NOT_FOUND, "Fant ikke fagsak for saksnummer: " + dto.saksnummer().getVerdi());
        }
        Fagsak fagsak = fagsakOpt.get();
        String aktørId = fagsak.getAktørId().getAktørId();

        List<UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO> deltakelser = ungdomsprogramRegisterKlient.hentForAktørId(aktørId).opplysninger();
        if (deltakelser.isEmpty()) {
            return feil(Response.Status.NOT_FOUND, "Fant ingen deltakelse for aktøren på saksnummer: " + dto.saksnummer().getVerdi());
        }
        if (deltakelser.size() > 1) {
            return feil(Response.Status.BAD_REQUEST, "Forventet én deltakelse, fant " + deltakelser.size() + " for saksnummer: " + dto.saksnummer().getVerdi());
        }
        UUID deltakelseId = deltakelser.getFirst().id();

        ungdomsprogramRegisterKlient.markerSomSøkt(aktørId, deltakelseId);

        entityManager.persist(new DiagnostikkFagsakLogg(fagsak.getId(), "/forvaltning/ungdomsprogram/marker-deltakelse-sokt", dto.begrunnelse().getTekst()));
        entityManager.flush();

        log.info("Manuelt markert deltakelse med deltakelseId={} som søkt for saksnummer={}.", deltakelseId, dto.saksnummer().getVerdi());

        return Response.ok().build();
    }

    private Response feil(Response.Status status, String melding) {
        log.warn(melding);
        return Response.status(status).entity(melding).build();
    }

    public record MarkerDeltakelseSøktRequest(
        @StandardAbacAttributt(StandardAbacAttributtType.SAKSNUMMER)
        @JsonProperty(value = SaksnummerDto.NAME, required = true)
        @NotNull
        @Valid
        Saksnummer saksnummer,

        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class)
        KortTekst begrunnelse
    ) {}
}
