package no.nav.foreldrepenger.web.app.tjenester.aktoer;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.web.app.exceptions.FeilDto;
import no.nav.foreldrepenger.web.app.exceptions.FeilType;
import no.nav.foreldrepenger.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.kontrakt.behandling.FagsakDto;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.kontrakt.person.AktørInfoDto;
import no.nav.k9.sak.kontrakt.person.PersonDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@ApplicationScoped
@Transactional
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
    @Operation(description = "Henter informasjon om en aktøer", tags = "aktoer", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i fpsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktørInfoDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path("/aktoer-info")
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getAktoerInfo(@NotNull @QueryParam("aktoerId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto aktoerIdDto) {
        var aktoerId = aktoerIdDto.getAktørId();
        AktørInfoDto aktoerInfoDto = new AktørInfoDto();
        if (aktoerId != null) {
            AktørId aktørId = new AktørId(aktoerId);
            Optional<Personinfo> personinfo = tpsTjeneste.hentBrukerForAktør(aktørId);
            if (personinfo.isPresent()) {
                Personinfo pi = personinfo.get();
                PersonDto personDto = new PersonDto(
                    pi.getNavn(),
                    pi.getAlder(),
                    String.valueOf(pi.getPersonIdent().getIdent()),
                    pi.erKvinne(),
                    pi.getPersonstatus(),
                    pi.getDiskresjonskode(),
                    pi.getDødsdato());
                aktoerInfoDto.setPerson(personDto);
                aktoerInfoDto.setAktoerId(pi.getAktørId().getId());
                List<FagsakDto> fagsakDtoer = new ArrayList<>();
                List<Fagsak> fagsaker = fagsakRepository.hentForBruker(aktørId);
                for (var f : fagsaker) {
                    fagsakDtoer.add(new FagsakDto(f.getSaksnummer(), f.getYtelseType(), f.getStatus(), personDto,
                        null, f.getSkalTilInfotrygd(),
                        f.getOpprettetTidspunkt(), f.getEndretTidspunkt()));
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
