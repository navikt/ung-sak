package no.nav.k9.sak.web.app.tjenester.saksnummer;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerEntitet;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.HentReservertSaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.ReserverSaksnummerDto;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;

@Path(SaksnummerRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class SaksnummerRestTjeneste {
    public static final String BASE_PATH = "/saksnummer";
    private static final String JSON_UTF8 = "application/json; charset=UTF-8";
    private static final Logger log = LoggerFactory.getLogger(SaksnummerRestTjeneste.class);

    private SaksnummerRepository saksnummerRepository;
    private ReservertSaksnummerRepository reservertSaksnummerRepository;
    private boolean enableReservertSaksnummer;

    public SaksnummerRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public SaksnummerRestTjeneste(SaksnummerRepository saksnummerRepository,
                                  ReservertSaksnummerRepository reservertSaksnummerRepository,
                                  @KonfigVerdi(value = "ENABLE_RESERVERT_SAKSNUMMER", defaultVerdi = "false") boolean enableReservertSaksnummer) {
        this.saksnummerRepository = saksnummerRepository;
        this.reservertSaksnummerRepository = reservertSaksnummerRepository;
        this.enableReservertSaksnummer = enableReservertSaksnummer;
    }

    @POST
    @Path("/reserver")
    @Produces(JSON_UTF8)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Reserver saksnummer.", summary = ("Reserver saksnummer"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto reserverSaksnummer(@Parameter(description = "ReserverSaksnummerDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ReserverSaksnummerDto dto) {
        if (!enableReservertSaksnummer) {
            throw new UnsupportedOperationException("Funksjonaliteten er avskrudd");
        }
        //TODO bør vi sjekke at aktørid er gyldig?
        final SaksnummerDto saksnummer = new SaksnummerDto(saksnummerRepository.genererNyttSaksnummer());
        reservertSaksnummerRepository.lagre(saksnummer.getVerdi().getVerdi(), dto.getYtelseType(), dto.getBrukerAktørId(), dto.getPleietrengendeAktørId());
        log.info("Reserverte saksnummer: " + saksnummer);
        return saksnummer;
    }

    @GET
    @Produces(JSON_UTF8)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent reservert saksnummer.", summary = ("Henter reservert saksnummer med ytelse, bruker og pleietrengende"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public HentReservertSaksnummerDto hentReservertSaksnummer(@Parameter(description = "SaksnummerDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto dto) {
        if (!enableReservertSaksnummer) {
            throw new UnsupportedOperationException("Funksjonaliteten er avskrudd");
        }
        final var entitet = reservertSaksnummerRepository.hent(dto.getVerdi().getVerdi());
        return mapTilDto(entitet);
    }

    private static HentReservertSaksnummerDto mapTilDto(ReservertSaksnummerEntitet entitet) {
        return new HentReservertSaksnummerDto(entitet.getSaksnummer().getVerdi(),
            entitet.getYtelseType(),
            entitet.getBrukerAktørId().getAktørId(),
            entitet.getPleietrengendeAktørId() != null ? entitet.getPleietrengendeAktørId().getAktørId() : null);
    }
}
