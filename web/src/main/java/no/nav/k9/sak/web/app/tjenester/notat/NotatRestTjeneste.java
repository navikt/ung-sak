package no.nav.k9.sak.web.app.tjenester.notat;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.notat.Notat;
import no.nav.k9.sak.behandlingslager.notat.NotatAktørEntitet;
import no.nav.k9.sak.behandlingslager.notat.NotatBuilder;
import no.nav.k9.sak.behandlingslager.notat.NotatRepository;
import no.nav.k9.sak.behandlingslager.notat.NotatSakEntitet;
import no.nav.k9.sak.kontrakt.notat.EndreNotatDto;
import no.nav.k9.sak.kontrakt.notat.NotatDto;
import no.nav.k9.sak.kontrakt.notat.NyttNotatDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path("")
@ApplicationScoped
@Transactional
public class NotatRestTjeneste {

    private NotatRepository notatRepository;
    private FagsakRepository fagsakRepository;

    @Inject
    public NotatRestTjeneste(NotatRepository notatRepository, FagsakRepository fagsakRepository) {
        this.notatRepository = notatRepository;
        this.fagsakRepository = fagsakRepository;
    }

    @GET
    @Path("/notat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter alle notater for fagsak", tags = "notat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<NotatDto> hentForFagsak(@QueryParam("fagsakId") long fagsakId) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);

        List<Notat> notater = notatRepository.hentForSakOgAktør(fagsakId, fagsak.getYtelseType(), fagsak.getPleietrengendeAktørId());

        return notater.stream().map(this::mapDto).collect(Collectors.toList());

    }

    @GET
    @Path("/notat/{notatId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter notat", tags = "notat")
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public NotatDto hent(@PathParam("notatId") UUID notatId) {
        var notatEntitet = notatRepository.hent(notatId);
        return mapDto(notatEntitet);

    }

    @POST
    @Path("/notat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lag nytt notat", tags = "notat")
    @BeskyttetRessurs(action = CREATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response opprett(@Parameter(description = "Nytt notat") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) NyttNotatDto nyttNotatDto) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(nyttNotatDto.fagsakId());
        Notat notat = opprettNotat(fagsak, nyttNotatDto.notatTekst(), nyttNotatDto.notatGjelderType());
        return Response.status(Response.Status.CREATED).entity(mapDto(notat)).build();

    }

    private Notat opprettNotat(Fagsak fagsak, String notatTekst, NotatGjelderType notatGjelderType) {
        var gjelderPleietrengende = notatGjelderType == NotatGjelderType.PLEIETRENGENDE;
        Notat entitet = NotatBuilder.of(fagsak, gjelderPleietrengende)
            .notatTekst(notatTekst)
            .build();
        notatRepository.opprett(entitet);
        return entitet;
    }

    @POST
    @Path("/notat/{notatId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Endre eksistrende notat", tags = "notat")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response endre(
        @PathParam("notatId") UUID notatId,
        @Parameter(description = "Notat som skal endres") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) EndreNotatDto endreNotatDto) {
//        var notat = notatRepository.hent(notatId);
//        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(notat.getFagsakId());
//
//        var nyttNotat = opprettNotat(fagsak, endreNotatDto.notatTekst(), endreNotatDto.notatGjelderType());
//
//        notat.erstattMed(nyttNotat.getId()); //todo constraint på erstattId + primær nøkkel?
//        notatRepository.oppdater(notat);

        return Response.status(Response.Status.CREATED).build();

    }

    @PUT
    @Path("/notat/{notatId}/toggle-skjul")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Skjul notat", tags = "notat")
    @BeskyttetRessurs(action = UPDATE, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response skjul(@PathParam("notatId") UUID notatId, @QueryParam("skjul") boolean skjul) {
        var notat = notatRepository.hent(notatId);
        notat.skjul(skjul);
        notatRepository.oppdater(notat);
        return Response.ok().build();

    }


    private NotatDto mapDto(Notat entitet) {
        //TODO to interface or not to interface, that is the question....
        if (entitet instanceof NotatAktørEntitet aktørEntitet) {
            return new NotatDto(
                aktørEntitet.getId(),
                aktørEntitet.getUuid(),
                aktørEntitet.getNotatTekst(),
                aktørEntitet.isSkjult(),
                NotatGjelderType.PLEIETRENGENDE,
                aktørEntitet.getVersjon(),
                aktørEntitet.getOpprettetAv(),
                aktørEntitet.getOpprettetTidspunkt(),
                aktørEntitet.getEndretAv(),
                aktørEntitet.getEndretTidspunkt());
        } else if (entitet instanceof NotatSakEntitet fagsakNotat) {
            return new NotatDto(
                fagsakNotat.getId(),
                fagsakNotat.getUuid(),
                fagsakNotat.getNotatTekst(),
                fagsakNotat.isSkjult(),
                NotatGjelderType.FAGSAK,
                fagsakNotat.getVersjon(),
                fagsakNotat.getOpprettetAv(),
                fagsakNotat.getOpprettetTidspunkt(),
                fagsakNotat.getEndretAv(),
                fagsakNotat.getEndretTidspunkt()
            );
        }

        throw new IllegalStateException("Utviklerfeil: Støtter ikke Notat typen");
    }

}
