package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.integrasjon.ldap.LdapBruker;
import no.nav.k9.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.saksbehandler.SaksbehandlerDto;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;
import no.nav.k9.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;
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
    private SykdomVurderingService sykdomVurderingService;

    public SaksbehandlerRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public SaksbehandlerRestTjeneste(
            @KonfigVerdi(value = "systembruker.username", required = false) String systembruker,
            HistorikkRepository historikkRepository,
            BehandlingRepository behandlingRepository,
            SykdomVurderingService sykdomVurderingService) {
        this.systembruker = systembruker;
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingService = sykdomVurderingService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for identer som har berørt en fagsak",
        tags = "nav-ansatt",
        summary = ("Identer hentes fra historikkinnslag og sykdomsvurderinger.")
    )
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    public SaksbehandlerDto getSaksbehandlere(
            @QueryParam(BehandlingUuidDto.NAME)
            @Parameter(description = BehandlingUuidDto.DESC)
            @NotNull
            @Valid
            @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class)
                BehandlingUuidDto behandlingUuid) {

        Behandling behandling = behandlingRepository.hentBehandlingHvisFinnes(behandlingUuid.getBehandlingUuid()).get();
        List<Historikkinnslag> historikkinnslag = historikkRepository.hentHistorikkForSaksnummer(behandling.getFagsak().getSaksnummer());

        LocalDateTimeline<SykdomVurderingVersjon> ktpTimeline = sykdomVurderingService.hentVurderinger(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        LocalDateTimeline<SykdomVurderingVersjon> toopTimeline = sykdomVurderingService.hentVurderinger(SykdomVurderingType.TO_OMSORGSPERSONER, behandling);

        Set<String> unikeIdenter = historikkinnslag.stream().map(i -> i.getOpprettetAv()).collect(Collectors.toSet());
        unikeIdenter.addAll(historikkinnslag.stream().map(i -> i.getEndretAv()).collect(Collectors.toSet()));
        unikeIdenter.addAll(ktpTimeline.stream().map(i -> i.getValue().getEndretAv()).collect(Collectors.toSet()));
        unikeIdenter.addAll(toopTimeline.stream().map(i -> i.getValue().getEndretAv()).collect(Collectors.toSet()));

        unikeIdenter.remove(systembruker);

        Map<String, String> identTilNavn = new HashMap<>();

        unikeIdenter.forEach(ident -> {
            if (ident != null) {
                if (!identTilNavn.containsKey(ident)) {
                    String saksbehandlerCachet = cache.get(ident);
                    if (saksbehandlerCachet != null) {
                        identTilNavn.put(ident, saksbehandlerCachet);
                    } else {
                        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
                        String brukernavn = ldapBruker.getDisplayName();
                        cache.put(ident, brukernavn);
                    }
                }
            }
        });

        return new SaksbehandlerDto(identTilNavn);
    }

}
