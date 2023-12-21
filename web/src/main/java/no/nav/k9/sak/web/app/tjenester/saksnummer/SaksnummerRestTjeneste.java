package no.nav.k9.sak.web.app.tjenester.saksnummer;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerAktørKoblingRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;

@Path(SaksnummerRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class SaksnummerRestTjeneste {
    public static final String BASE_PATH = "/saksnummer";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final Logger log = LoggerFactory.getLogger(SaksnummerRestTjeneste.class);

    private SaksnummerRepository saksnummerRepository;
    private SaksnummerAktørKoblingRepository saksnummerAktørKoblingRepository;
    private boolean enableReservertSaksnummer;

    public SaksnummerRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public SaksnummerRestTjeneste(SaksnummerRepository saksnummerRepository,
                                  SaksnummerAktørKoblingRepository saksnummerAktørKoblingRepository,
                                  @KonfigVerdi(value = "ENABLE_RESERVERT_SAKSNUMMER", defaultVerdi = "false") boolean enableReservertSaksnummer) {
        this.saksnummerRepository = saksnummerRepository;
        this.saksnummerAktørKoblingRepository = saksnummerAktørKoblingRepository;
        this.enableReservertSaksnummer = enableReservertSaksnummer;
    }

    @POST
    @Path("/reserver")
    @Produces(JSON_UTF8)
    @Operation(description = "Reserver saksnummer.", summary = ("Reserver saksnummer"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto reserverSaksnummer() {
        if (!enableReservertSaksnummer) {
            throw new UnsupportedOperationException("Funksjonaliteten er avskrudd");
        }
        final SaksnummerDto saksnummer = new SaksnummerDto(saksnummerRepository.genererNyttSaksnummer());
        log.info("Reserverte saksnummer: " + saksnummer);
        return saksnummer;
    }
}
