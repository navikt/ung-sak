package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.concurrent.TimeUnit;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.integrasjon.ldap.LdapBruker;
import no.nav.k9.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.sak.kontrakt.saksbehandler.SaksbehandlerDto;
import no.nav.k9.sak.kontrakt.saksbehandler.SaksbehandlerQueryDto;

@Path("/saksbehandler")
@ApplicationScoped
@Transactional
public class SaksbehandlerRestTjeneste {
    public static final String SAKSBEHANDLER_PATH = "/saksbehandler";
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(120, TimeUnit.MINUTES);

    private LRUCache<String, SaksbehandlerDto> cache = new LRUCache<>(100, CACHE_ELEMENT_LIVE_TIME_MS);

    @Inject
    public SaksbehandlerRestTjeneste() {
        //NOSONAR
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for ident",
        tags = "nav-ansatt",
        summary = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging.")
    )
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    public SaksbehandlerDto getBruker(@NotNull @QueryParam("brukerid") @Parameter(description = "brukerid") @Valid SaksbehandlerQueryDto ident) {
        SaksbehandlerDto saksbehandlerCachet = cache.get(ident.getBrukerid());
        if (saksbehandlerCachet != null) {
            return saksbehandlerCachet;
        }

        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident.getBrukerid());
        SaksbehandlerDto saksbehandlerDto = new SaksbehandlerDto(ldapBruker.getDisplayName());
        cache.put(ident.getBrukerid(), saksbehandlerDto);
        return saksbehandlerDto;
    }
}
