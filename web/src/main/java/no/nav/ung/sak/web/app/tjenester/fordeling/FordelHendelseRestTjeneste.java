package no.nav.ung.sak.web.app.tjenester.fordeling;

import static no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.prosesstask.api.PollTaskAfterTransaction;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelsemottakTjeneste;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.hendelser.HendelseDto;
import no.nav.ung.sak.kontrakt.hendelser.PåvirkedeSaker;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;

/**
 * Mottar hendelser fra k9-fordel
 */
@Path("")
@ApplicationScoped
@Transactional
public class FordelHendelseRestTjeneste {

    private static final String JSON_UTF8 = "application/json; charset=UTF-8";

    private HendelsemottakTjeneste hendelsemottakTjeneste;

    public FordelHendelseRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public FordelHendelseRestTjeneste(HendelsemottakTjeneste hendelsemottakTjeneste) {
        this.hendelsemottakTjeneste = hendelsemottakTjeneste;
    }


    @POST
    @Path("/fagsak/hendelse/sok")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Finn saker påvirket av hendelse", summary = ("Finn saker påvirket av hendelse"), tags = "fordel")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public PåvirkedeSaker finnPåvirkedeFagsaker(@Parameter(description = "Oppretter fagsak") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) HendelseDto dto) {
        var hendelse = dto.getHendelse();

        var fagsaker = hendelsemottakTjeneste.finnFagsakerTilVurdering(hendelse).keySet();

        var saksnummere = fagsaker.stream().map(it -> new SaksnummerDto(it.getSaksnummer())).collect(Collectors.toList());
        return new PåvirkedeSaker(saksnummere);
    }

    @POST
    @Path("/fagsak/hendelse/innsending")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(JSON_UTF8)
    @Operation(description = "Mottak av dokument.", tags = "fordel")
    @PollTaskAfterTransaction
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public PåvirkedeSaker innsending(@Parameter(description = "Hendelse innsendt.") @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) @Valid HendelseDto dto) {
        var hendelse = dto.getHendelse();

        var fagsaker = hendelsemottakTjeneste.mottaHendelse(hendelse).keySet();

        var saksnummere = fagsaker.stream().map(it -> new SaksnummerDto(it.getSaksnummer())).collect(Collectors.toList());
        return new PåvirkedeSaker(saksnummere);
    }
}
