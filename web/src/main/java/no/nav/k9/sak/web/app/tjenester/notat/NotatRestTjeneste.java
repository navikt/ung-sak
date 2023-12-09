package no.nav.k9.sak.web.app.tjenester.notat;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.notat.NotatAktørEntitet;
import no.nav.k9.sak.behandlingslager.notat.NotatBuilder;
import no.nav.k9.sak.behandlingslager.notat.NotatEntitet;
import no.nav.k9.sak.behandlingslager.notat.NotatRepository;
import no.nav.k9.sak.behandlingslager.notat.NotatSakEntitet;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.notat.EndreNotatDto;
import no.nav.k9.sak.kontrakt.notat.NotatDto;
import no.nav.k9.sak.kontrakt.notat.OpprettNotatDto;
import no.nav.k9.sak.kontrakt.notat.SkjulNotatDto;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Path("")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class NotatRestTjeneste {

    private NotatRepository notatRepository;
    private FagsakRepository fagsakRepository;


    @Inject
    public NotatRestTjeneste(NotatRepository notatRepository, FagsakRepository fagsakRepository) {
        this.notatRepository = notatRepository;
        this.fagsakRepository = fagsakRepository;
    }

    NotatRestTjeneste() {
    }

    @GET
    @Path("/notat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter alle notater for fagsak", tags = "notat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hent(
        @NotNull @QueryParam(SaksnummerDto.NAME) @Parameter(description = "Saksnummer") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto saksnummer,
        @QueryParam("notatId") @Parameter(description = "Notat uuid")  @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) UUID notatId
    ) {
        if (notatId != null) {
            NotatEntitet notat = hentNotat(saksnummer.getVerdi(), notatId);
            return Response.ok().entity(Collections.singleton(mapDto(notat))).build();
        }

        List<NotatEntitet> notater = hentAlle(saksnummer.getVerdi());
        return Response.ok().entity(notater.stream().map(this::mapDto).collect(Collectors.toList())).build();

    }

    @POST
    @Path("/notat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lag nytt notat", tags = "notat")
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response opprett(
        @NotNull @Parameter(description = "Nytt notat") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) OpprettNotatDto opprettNotatDto
    ) {
        Fagsak fagsak = hentSak(opprettNotatDto.saksnummer().getSaksnummer());

        var gjelderPleietrengende = opprettNotatDto.notatGjelderType() == NotatGjelderType.PLEIETRENGENDE;
        NotatEntitet entitet = NotatBuilder.of(fagsak, gjelderPleietrengende)
            .notatTekst(opprettNotatDto.notatTekst())
            .build();
        notatRepository.lagre(entitet);
        return Response.status(Response.Status.CREATED).entity(mapDto(entitet)).build();

    }

    @POST
    @Path("/notat/endre")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endre eksistrende notat", tags = "notat")
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response endre(
       @NotNull @Parameter(description = "Notat som skal endres") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) EndreNotatDto endreNotatDto
    ) {
        var notat = hentNotatForEndring(endreNotatDto.saksnummer().getSaksnummer(), endreNotatDto.notatId(), endreNotatDto.versjon());

        validerTilgangForEndring(notat);

        if (!notat.getNotatTekst().equals(endreNotatDto.notatTekst())) {
            notat.nyTekst(endreNotatDto.notatTekst());
            notatRepository.lagre(notat);
        }

        return Response.status(Response.Status.OK).entity(mapDto(notat)).build();

    }

    private void validerTilgangForEndring(NotatEntitet notat) {
        if (!notat.kanRedigere(saksbehandlerUserid())) {
            throw NotatFeil.FACTORY.eierIkkeNotat().toException();
        }
    }

    private static String saksbehandlerUserid() {
        return Optional.ofNullable(SubjectHandler.getSubjectHandler().getUid()).orElseThrow();
    }


    @POST
    @Path("/notat/skjul")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Skjul notat", tags = "notat")
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response skjul(
        @NotNull @Parameter(description = "Notat som skal skjules") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SkjulNotatDto skjulNotatDto
    ) {
        var notat = hentNotatForEndring(skjulNotatDto.saksnummer().getSaksnummer(), skjulNotatDto.notatId(), skjulNotatDto.versjon());
        if (notat.isSkjult() != skjulNotatDto.skjul()) {
            notat.skjul(skjulNotatDto.skjul());
            notatRepository.lagre(notat);
        }

        return  Response.status(Response.Status.OK).entity(mapDto(notat)).build();

    }

    //Notat må hentes basert på saksnummer for tilgangssjekk!
    private NotatEntitet hentNotat(Saksnummer saksnummer, UUID notatUuid) {
        var notaterPåSak = hentAlle(saksnummer);

        var notater = notaterPåSak.stream()
            .filter(it -> it.getUuid().equals(notatUuid))
            .toList();

        if (notater.size() > 1) {
            throw new IllegalStateException("Utvilkerfeil: Flere notater med samme id");
        }

        if (notater.isEmpty()) {
            throw NotatFeil.FACTORY.fantIkkeNotat().toException();
        }

        return notater.get(0);
    }

    private NotatEntitet hentNotatForEndring(Saksnummer saksnummer, UUID notatUuid, long versjon) {
        var notat = hentNotat(saksnummer, notatUuid);
        if (notat.getVersjon() != versjon) {
            throw NotatFeil.FACTORY.notatUtdatert(versjon, notat.getVersjon()).toException();
        }

        return notat;
    }

    private List<NotatEntitet> hentAlle(Saksnummer saksnummer) {
        var fagsak = hentSak(saksnummer);

        return notatRepository.hentForSakOgAktør(fagsak);
    }


    private Fagsak hentSak(Saksnummer saksnummer) {
        return fagsakRepository.hentSakGittSaksnummer(saksnummer)
            .orElseThrow(() -> NotatFeil.FACTORY.fantIkkeSak().toException());
    }

    private NotatDto mapDto(NotatEntitet entitet) {
        return new NotatDto(
                entitet.getUuid(),
                entitet.getNotatTekst(),
                entitet.isSkjult(),
                bestemNotatGjelder(entitet),
                entitet.kanRedigere(saksbehandlerUserid()),
                entitet.getVersjon(),
                entitet.getOpprettetAv(),
                entitet.getOpprettetTidspunkt(),
                entitet.getNotatTekstEndretAv(),
                entitet.getNotatTekstEndretTidspunkt());
    }

    private static NotatGjelderType bestemNotatGjelder(NotatEntitet entitet) {
        if (entitet instanceof NotatAktørEntitet) {
            return NotatGjelderType.PLEIETRENGENDE;
        }
        if (entitet instanceof NotatSakEntitet) {
            return NotatGjelderType.FAGSAK;
        }
        throw new IllegalStateException("Utviklerfeil: Støtter ikke Notat typen");
    }


}
