package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.Collection;
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
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.BooleanUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.k9.felles.integrasjon.ldap.LdapBruker;
import no.nav.k9.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.sak.kontrakt.abac.InnloggetAnsattDto;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentDto;
import no.nav.k9.sak.kontrakt.dokument.DokumentIdDto;
import no.nav.k9.sak.kontrakt.dokument.JournalpostIdDto;
import no.nav.k9.sak.kontrakt.saksbehandler.SaksbehandlerDto;
import no.nav.k9.sak.web.app.util.LdapUtil;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@Path("/saksbehandler")
@ApplicationScoped
@Transactional
public class SaksbehandlerRestTjeneste {
    public static final String SAKSBEHANDLER_PATH = "/saksbehandler";
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES);

    private LRUCache<String, SaksbehandlerDto> cache = new LRUCache<>(10, CACHE_ELEMENT_LIVE_TIME_MS);

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
    public SaksbehandlerDto getBruker(@NotNull @QueryParam("brukerid") @Parameter(description = "brukerid") @Valid String ident) {
        SaksbehandlerDto saksbehandlerCachet = cache.get(ident);
        if (saksbehandlerCachet != null) {
            return saksbehandlerCachet;
        }

        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        SaksbehandlerDto saksbehandlerDto = new SaksbehandlerDto(ldapBruker.getDisplayName());
        cache.put(ident, saksbehandlerDto);
        return saksbehandlerDto;
    }
}
