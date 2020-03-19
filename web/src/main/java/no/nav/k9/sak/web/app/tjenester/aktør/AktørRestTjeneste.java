package no.nav.k9.sak.web.app.tjenester.aktør;

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
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.kontrakt.FeilDto;
import no.nav.k9.sak.kontrakt.FeilType;
import no.nav.k9.sak.kontrakt.behandling.FagsakDto;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.kontrakt.person.AktørInfoDto;
import no.nav.k9.sak.kontrakt.person.PersonDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

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
            @ApiResponse(responseCode = "200", description = "Returnerer basisinformasjon om en aktør og hvilke fagsaker vedkommede har i fpsak.", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AktørInfoDto.class)))
    })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Path("/aktoer-info")
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
                    pi.getAlder(),
                    String.valueOf(pi.getPersonIdent().getIdent()),
                    pi.erKvinne(),
                    pi.getPersonstatus(),
                    pi.getDiskresjonskode(),
                    pi.getDødsdato());
                aktoerInfoDto.setPerson(personDto);
                aktoerInfoDto.setAktørId(pi.getAktørId());
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
