package no.nav.k9.sak.web.app.tjenester.saksnummer;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerEntitet;
import no.nav.k9.sak.behandlingslager.saksnummer.ReservertSaksnummerRepository;
import no.nav.k9.sak.behandlingslager.saksnummer.SaksnummerRepository;
import no.nav.k9.sak.domene.person.pdl.AktørTjeneste;
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.HentReservertSaksnummerDto;
import no.nav.k9.sak.kontrakt.mottak.ReserverSaksnummerDto;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;
import no.nav.k9.sak.typer.AktørId;
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
    private AktørTjeneste aktørTjeneste;
    private boolean enableReservertSaksnummer;

    public SaksnummerRestTjeneste() {// For Rest-CDI
    }

    @Inject
    public SaksnummerRestTjeneste(SaksnummerRepository saksnummerRepository,
                                  ReservertSaksnummerRepository reservertSaksnummerRepository,
                                  AktørTjeneste aktørTjeneste,
                                  @KonfigVerdi(value = "ENABLE_RESERVERT_SAKSNUMMER", defaultVerdi = "false") boolean enableReservertSaksnummer) {
        this.saksnummerRepository = saksnummerRepository;
        this.reservertSaksnummerRepository = reservertSaksnummerRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.enableReservertSaksnummer = enableReservertSaksnummer;
    }

    @POST
    @Path("/reserver")
    @Produces(JSON_UTF8)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Reserver saksnummer.", summary = ("Reserver saksnummer"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.CREATE, resource = FAGSAK)
    public SaksnummerDto reserverSaksnummer(@NotNull @Parameter(description = "ReserverSaksnummerDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) ReserverSaksnummerDto dto) {
        if (!enableReservertSaksnummer) {
            throw new UnsupportedOperationException("Funksjonaliteten er avskrudd");
        }
        validerReservasjon(dto);

        SaksnummerDto saksnummer;
        var eksisterende = reservertSaksnummerRepository.hent(dto.getYtelseType(), dto.getBrukerAktørId(), dto.getPleietrengendeAktørId(), dto.getRelatertPersonAktørId(), dto.getBehandlingsår());
        if (eksisterende.isPresent()) {
            saksnummer = new SaksnummerDto(eksisterende.get().getSaksnummer());
            log.info("Returnerer eksisterende reservert saksnummer: " + saksnummer);
        } else {
            saksnummer = new SaksnummerDto(saksnummerRepository.genererNyttSaksnummer());
            reservertSaksnummerRepository.lagre(saksnummer.getVerdi(), dto.getYtelseType(), dto.getBrukerAktørId(), dto.getPleietrengendeAktørId(), dto.getRelatertPersonAktørId(), dto.getBehandlingsår());
            log.info("Reserverte nytt saksnummer: " + saksnummer);
        }

        return saksnummer;
    }

    @GET
    @Produces(JSON_UTF8)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent reservert saksnummer.", summary = ("Henter reservert saksnummer med ytelse, bruker og pleietrengende"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public HentReservertSaksnummerDto hentReservertSaksnummer(@NotNull @QueryParam("saksnummer") @Parameter(description = "SaksnummerDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) SaksnummerDto dto) {
        if (!enableReservertSaksnummer) {
            throw new UnsupportedOperationException("Funksjonaliteten er avskrudd");
        }
        final var entitet = reservertSaksnummerRepository.hent(dto.getVerdi());
        return entitet.map(SaksnummerRestTjeneste::mapTilDto).orElse(null);
    }

    @POST
    @Path("/søker")
    @Produces(JSON_UTF8)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent alle reserverte saksnummer på søker.", summary = ("Henter reserverte saksnummer med ytelse, bruker og pleietrengende"), tags = "saksnummer")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = FAGSAK)
    public List<HentReservertSaksnummerDto> hentReserverteSaksnummerPåSøker(@NotNull @Parameter(description = "AktørIdDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) AktørIdDto dto) {
        if (!enableReservertSaksnummer) {
            throw new UnsupportedOperationException("Funksjonaliteten er avskrudd");
        }
        final var entiteter = reservertSaksnummerRepository.hent(dto.getAktørId());
        return entiteter.stream().map(SaksnummerRestTjeneste::mapTilDto).toList();
    }

    private static HentReservertSaksnummerDto mapTilDto(ReservertSaksnummerEntitet entitet) {
        return new HentReservertSaksnummerDto(entitet.getSaksnummer().getVerdi(),
            entitet.getYtelseType(),
            entitet.getBrukerAktørId().getAktørId(),
            entitet.getPleietrengendeAktørId() != null ? entitet.getPleietrengendeAktørId().getAktørId() : null,
            entitet.getRelatertPersonAktørId() != null ? entitet.getRelatertPersonAktørId().getAktørId() : null,
            entitet.getBehandlingsår());
    }

    private void validerReservasjon(ReserverSaksnummerDto dto) {
        if (List.of(FagsakYtelseType.OMSORGSPENGER, FagsakYtelseType.OMSORGSPENGER_AO, FagsakYtelseType.OMSORGSPENGER_KS, FagsakYtelseType.OMSORGSPENGER_MA).contains(dto.getYtelseType())) {
            if (dto.getBehandlingsår() == null) {
                throw new IllegalArgumentException("Behandlingsår er påkrevd for omsorgspenger");
            }
        } else if (dto.getBehandlingsår() != null) {
            throw new IllegalArgumentException("Støtter ikke behandlingsår for " + dto.getYtelseType());
        }

        sjekkAktørIdMotPdl(dto.getBrukerAktørId());
        if (dto.getPleietrengendeAktørId() != null) {
            if (List.of(FagsakYtelseType.OMSORGSPENGER, FagsakYtelseType.OMSORGSPENGER_MA).contains(dto.getYtelseType())) {
                throw new IllegalArgumentException("Støtter ikke pleietrengende for " + dto.getYtelseType());
            }
            sjekkAktørIdMotPdl(dto.getPleietrengendeAktørId());
        }
        if (dto.getRelatertPersonAktørId() != null) {
            if (!dto.getYtelseType().equals(FagsakYtelseType.OMSORGSPENGER_MA)) {
                throw new IllegalArgumentException("Støtter ikke relatert person for " + dto.getYtelseType());
            }
            sjekkAktørIdMotPdl(dto.getRelatertPersonAktørId());
        }
    }

    private void sjekkAktørIdMotPdl(String aktørId) {
        if (aktørTjeneste.hentPersonIdentForAktørId(new AktørId(aktørId)).isEmpty()) {
            throw new IllegalArgumentException("Finner ikke oppgitt aktørId i PDL");
        }
    }
}
