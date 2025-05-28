package no.nav.ung.sak.web.app.tjenester.etterlysning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.api.PollTaskAfterTransaction;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.etterlysning.EtterlysningProssesseringTjeneste;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.etterlysning.EndreFristForEtterlysningDto;
import no.nav.ung.sak.kontrakt.etterlysning.Etterlysning;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.app.rest.Redirect;
import no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt.BehandlingsutredningApplikasjonTjeneste;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

@Path("")
@ApplicationScoped
@Transactional
public class EtterlysningRestTjeneste {

    public static final String PATH = "/behandling";
    public static final String ETTERLYSNINGER_PATH = PATH + "/etterlysninger";
    public static final String ENDRE_FRIST_PATH = PATH + "/etterlysninger/endre-frist";

    private EtterlysningRepository etterlysningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste;

    public EtterlysningRestTjeneste() {
        // For Rest-CDI
    }

    @Inject
    public EtterlysningRestTjeneste(EtterlysningRepository etterlysningRepository, BehandlingRepository behandlingRepository, BehandlingProsesseringTjeneste behandlingProsesseringTjeneste, BehandlingsutredningApplikasjonTjeneste behandlingsutredningApplikasjonTjeneste) {
        this.etterlysningRepository = etterlysningRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.behandlingsutredningApplikasjonTjeneste = behandlingsutredningApplikasjonTjeneste;
    }

    @GET
    @Path(ETTERLYSNINGER_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<Etterlysning> hentEtterlysninger(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var mappetEtterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId())
            .stream()
            .map(it -> new Etterlysning(it.getStatus(), it.getType(), new Periode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()), it.getEksternReferanse()))
            .toList();
        return mappetEtterlysninger;
    }

    @POST
    @Path(ENDRE_FRIST_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endrer frist for etterlysning", tags = "etterlysning")
    @PollTaskAfterTransaction
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    public Response endreFrist(@Context
                               HttpServletRequest request,
                               @Parameter(description = "Liste over etterlysninger med ny frist.")
                               @Valid
                               @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) EndreFristForEtterlysningDto endreFristForEtterlysningDto) throws URISyntaxException {

        final var behandlingId = endreFristForEtterlysningDto.getBehandlingId();
        final var behandling = behandlingRepository.hentBehandling(behandlingId);
        behandlingsutredningApplikasjonTjeneste.kanEndreBehandling(behandlingId, endreFristForEtterlysningDto.getBehandlingVersjon());
        endreFristForEtterlysningDto.getEndretFrister().forEach(it -> {
            final var etterlysning = etterlysningRepository.hentEtterlysningForEksternReferanse(it.etterlysningEksternReferanse());
            if (!etterlysning.getStatus().equals(EtterlysningStatus.VENTER)) {
                throw new IllegalArgumentException("Kan ikke endre frist for etterlysning dersom den ikke er i status VENTER. Status er " + etterlysning.getStatus());
            }
            // Anser det som usannsynlig at noen vil endre frist for etterlysning mer enn 14 dager frem i tid, så vi kaster en exception dersom det skjer siden det mest sannsynlig er gjort ved en feil
            if (it.frist().atStartOfDay().isAfter(LocalDateTime.now().plusDays(14))) {
                throw new IllegalArgumentException("Frist for etterlysning kan ikke settes mer enn 14 dager frem i tid.");
            }
            etterlysning.setFrist(it.frist().atStartOfDay());
            // Det vil mest sannsynlig bare være en etterlysning her, så vi anser det som ok å lagre inne i loopen for lesbarhet
            etterlysningRepository.lagre(etterlysning);
        });
        // Oppretter gjenoppta task for å oppdatere frist på aksjonspunkt (gjøres automatisk ved kjøring av kompletthetsteg)
        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
        return Redirect.tilBehandlingPollStatus(request, behandling.getUuid());
    }


}
