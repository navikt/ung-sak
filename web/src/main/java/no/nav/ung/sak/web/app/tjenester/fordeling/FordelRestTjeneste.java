package no.nav.ung.sak.web.app.tjenester.fordeling;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.mottak.FinnSak;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

import java.util.function.Function;

/**
 * Mottar dokumenter fra k9-fordel og k9-punsj og håndterer dispatch internt for saksbehandlingsløsningen.
 */
@Path(FordelRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class FordelRestTjeneste {

    static final String BASE_PATH = "/fordel";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private FagsakTjeneste fagsakTjeneste;

    public FordelRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelRestTjeneste(FagsakTjeneste fagsakTjeneste) {
        this.fagsakTjeneste = fagsakTjeneste;
    }


    @POST
    @Path("/fagsak/sok")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Finn eksisterende sak.", summary = ("Finn eksisterende fagsak"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionType.CREATE, resource = BeskyttetRessursResourceType.FAGSAK)
    public SaksnummerDto finnEksisterendeFagsak(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) FinnSak finnSakDto) {
        var ytelseType = finnSakDto.getYtelseType();

        AktørId bruker = finnSakDto.getAktørId();
        var periode = finnSakDto.getPeriode();

        var fagsak = fagsakTjeneste.finnesEnFagsakSomOverlapper(ytelseType, bruker, periode.getFom(), periode.getTom());

        return fagsak.isPresent() ? new SaksnummerDto(fagsak.get().getSaksnummer().getVerdi()) : null;
    }

    @POST
    @Path("/relatertSak")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(
        description = "Sjekker om det finnes en eksisterende fagsak med søker, pleietrengende og/eller relatert part.",
        summary = ("Sjekker om det finnes en eksisterende fagsak med søker, pleietrengende og/eller relatert part."),
        tags = "fordel"
    )
    @BeskyttetRessurs(action = BeskyttetRessursActionType.READ, resource = BeskyttetRessursResourceType.APPLIKASJON)
    public boolean finnesEksisterendeFagsakMedEnAvAktørene(@Parameter(description = "Søkeparametere") @TilpassetAbacAttributt(supplierClass = FordelRestTjeneste.AbacDataSupplier.class) @Valid FinnSak finnSakDto) {
        return fagsakTjeneste.finnesEnFagsakForMinstEnAvAktørene(
            finnSakDto.getYtelseType(),
            finnSakDto.getAktørId(),
            finnSakDto.getPeriode().getFom(),
            finnSakDto.getPeriode().getTom()
        );
    }


    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }





}
