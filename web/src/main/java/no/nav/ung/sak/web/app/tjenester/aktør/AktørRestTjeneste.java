package no.nav.ung.sak.web.app.tjenester.aktør;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.kontrakt.FeilDto;
import no.nav.ung.sak.kontrakt.FeilType;
import no.nav.ung.sak.kontrakt.fagsak.FagsakDto;
import no.nav.ung.sak.kontrakt.person.AktørIdDto;
import no.nav.ung.sak.kontrakt.person.AktørInfoDto;
import no.nav.ung.sak.kontrakt.person.PersonDto;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@ApplicationScoped
@Transactional
@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class AktørRestTjeneste {

    private FagsakRepository fagsakRepository;
    private TpsTjeneste tpsTjeneste;

    public AktørRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktørRestTjeneste(FagsakRepository fagsakRepository, TpsTjeneste tpsTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    @GET
    @Operation(description = "Henter informasjon om en aktøer", tags = "aktoer", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i ung-sak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktørInfoDto.class)))
    })
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path("/aktoer-info")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = FeilDto.class))),
        @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = FeilDto.class))),
    })
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAktoerInfo(@NotNull @QueryParam("aktoerId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktørIdDto) {
        AktørInfoDto aktoerInfoDto = new AktørInfoDto();
        if (aktørIdDto != null) {
            var aktørId = aktørIdDto.getAktørId();
            Optional<Personinfo> personinfo = tpsTjeneste.hentBrukerForAktør(aktørId);
            if (personinfo.isPresent()) {
                Personinfo pi = personinfo.get();
                PersonDto personDto = new PersonDto(
                    pi.getNavn(),
                    pi.getAlderIDag(),
                    String.valueOf(pi.getPersonIdent().getIdent()),
                    pi.getDiskresjonskode(),
                    pi.getDødsdato(),
                    pi.getAktørId()
                );
                aktoerInfoDto.setPerson(personDto);
                aktoerInfoDto.setAktørId(pi.getAktørId());
                List<FagsakDto> fagsakDtoer = new ArrayList<>();
                List<Fagsak> fagsaker = fagsakRepository.hentForBruker(aktørId);
                for (var f : fagsaker) {
                    var periode = new Periode(f.getPeriode().getFomDato(), f.getPeriode().getTomDato());
                    fagsakDtoer.add(new FagsakDto(
                        f.getSaksnummer(),
                        f.getYtelseType(),
                        f.getStatus(),
                        periode,
                        personDto,
                        null,
                        f.getOpprettetTidspunkt(),
                        f.getEndretTidspunkt())
                    );
                }
                aktoerInfoDto.setFagsaker(fagsakDtoer);
                return Response.ok(aktoerInfoDto).build();
            } else {
                FeilDto feilDto = new FeilDto(FeilType.TOMT_RESULTAT_FEIL, "Finner ingen aktør med angitt ident.");
                return Response.ok(feilDto).status(404).build();
            }
        } else {
            FeilDto feilDto = new FeilDto(FeilType.GENERELL_FEIL, "Query parameteret 'aktoerId' mangler i forespørselen.");
            return Response.ok(feilDto).status(400).build();
        }

    }

}
