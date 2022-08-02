package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.exception.VLException;
import no.nav.k9.felles.integrasjon.ldap.LdapBruker;
import no.nav.k9.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.saksbehandler.SaksbehandlerDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

@Path("/saksbehandler")
@ApplicationScoped
@Transactional
public class SaksbehandlerRestTjeneste {
    public static final String SAKSBEHANDLER_PATH = "/saksbehandler";
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(480, TimeUnit.MINUTES);

    private LRUCache<String, String> cache = new LRUCache<>(100, CACHE_ELEMENT_LIVE_TIME_MS);

    private String systembruker;

    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;
    private SykdomVurderingTjeneste sykdomVurderingTjeneste;

    public SaksbehandlerRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public SaksbehandlerRestTjeneste(
        @KonfigVerdi(value = "systembruker.username", required = false) String systembruker,
        HistorikkRepository historikkRepository,
        BehandlingRepository behandlingRepository,
        SykdomVurderingTjeneste sykdomVurderingTjeneste) {
        this.systembruker = systembruker;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingTjeneste = sykdomVurderingTjeneste;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for identer som har berørt en fagsak",
        tags = "nav-ansatt",
        summary = ("Identer hentes fra historikkinnslag og sykdomsvurderinger.")
    )
    @BeskyttetRessurs(action = READ, resource = FAGSAK, sporingslogg = false)
    public SaksbehandlerDto getSaksbehandlere(
        @QueryParam(BehandlingUuidDto.NAME)
        @Parameter(description = BehandlingUuidDto.DESC)
        @NotNull
        @Valid
        @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
        BehandlingUuidDto behandlingUuid) {

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).orElseThrow();
        List<Historikkinnslag> historikkinnslag = historikkRepository.hentHistorikkForSaksnummer(behandling.getFagsak().getSaksnummer());

        LocalDateTimeline<SykdomVurderingVersjon> ktpTimeline = sykdomVurderingTjeneste.hentVurderinger(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        LocalDateTimeline<SykdomVurderingVersjon> toopTimeline = sykdomVurderingTjeneste.hentVurderinger(SykdomVurderingType.TO_OMSORGSPERSONER, behandling);

        Set<String> unikeIdenter = historikkinnslag.stream()
            .map(BaseEntitet::getOpprettetAv)
            .collect(Collectors.toSet());

        unikeIdenter.addAll(historikkinnslag.stream()
            .map(BaseEntitet::getEndretAv)
            .collect(Collectors.toSet()));

        unikeIdenter.addAll(ktpTimeline.stream()
            .map(LocalDateSegment::getValue)
            .map(SykdomVurderingVersjon::getEndretAv)
            .collect(Collectors.toSet()));

        unikeIdenter.addAll(toopTimeline.stream()
            .map(LocalDateSegment::getValue)
            .map(SykdomVurderingVersjon::getEndretAv)
            .collect(Collectors.toSet()));

        unikeIdenter.remove(systembruker);

        Map<String, String> identTilNavn = new HashMap<>();

        unikeIdenter.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .forEach(ident -> hentNavForIdent(identTilNavn, ident));

        return new SaksbehandlerDto(identTilNavn);
    }

    private void hentNavForIdent(Map<String, String> identTilNavn, String ident) {
        if (!identTilNavn.containsKey(ident)) {
            String saksbehandlerCachet = cache.get(ident);
            if (saksbehandlerCachet == null) {
                try {
                    LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
                    String brukernavn = ldapBruker.getDisplayName();
                    cache.put(ident, brukernavn);
                    saksbehandlerCachet = brukernavn;
                } catch (VLException e) {
                    // Feil mot LDAP
                }
            }
            if (saksbehandlerCachet != null) {
                identTilNavn.put(ident, saksbehandlerCachet);
            }
        }
    }

}
