package no.nav.foreldrepenger.web.app.tjenester.aktoer;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@ApplicationScoped
@Transaction
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class AktoerRestTjeneste {

    private FagsakRepository fagsakRepository;
    private TpsTjeneste tpsTjeneste;

    public AktoerRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktoerRestTjeneste(FagsakRepository fagsakRepository, TpsTjeneste tpsTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    @GET
    @Operation(description = "Henter informasjon om en aktøer",
        tags = "aktoer",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i fpsak.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = AktoerInfoDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path("/aktoer-info")
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAktoerInfo(@NotNull @QueryParam("aktoerId") @Valid AktoerIdDto aktoerIdDto) {
        Optional<AktørId> aktoerId = aktoerIdDto.get();
        AktoerInfoDto aktoerInfoDto = new AktoerInfoDto();
        if (aktoerId.isPresent()) {
            Optional<Personinfo> personinfo = tpsTjeneste.hentBrukerForAktør(aktoerId.get());
            if (personinfo.isPresent()) {
                Personinfo pi = personinfo.get();
                PersonDto personDto = new PersonDto(
                    pi.getNavn(),
                    pi.getAlder(),
                    String.valueOf(pi.getPersonIdent().getIdent()),
                    pi.erKvinne(),
                    pi.getPersonstatus(),
                    pi.getDiskresjonskode(),
                    pi.getDødsdato()
                );
                aktoerInfoDto.setPerson(personDto);
                aktoerInfoDto.setAktoerId(pi.getAktørId().getId());
                List<FagsakDto> fagsakDtoer = new ArrayList<>();
                List<Fagsak> fagsaker = fagsakRepository.hentForBruker(aktoerId.get());
                for (Fagsak fagsak : fagsaker) {
                    fagsakDtoer.add(new FagsakDto(
                        fagsak,
                        null,
                        null,
                        null,
                        null,
                        fagsak.getSkalTilInfotrygd()));
                }
                aktoerInfoDto.setFagsaker(fagsakDtoer);
                return Response.ok(aktoerInfoDto).build();
            } else {
                FeilDto feilDto = new FeilDto(FeilType.TOMT_RESULTAT_FEIL, "Finner ingen aktør med denne ideen.");
                return Response.ok(feilDto).status(404).build();
            }
        } else {
            FeilDto feilDto = new FeilDto(FeilType.GENERELL_FEIL, "Query parameteret 'aktoerId' mangler i forespørselen.");
            return Response.ok(feilDto).status(400).build();
        }

    }

}
