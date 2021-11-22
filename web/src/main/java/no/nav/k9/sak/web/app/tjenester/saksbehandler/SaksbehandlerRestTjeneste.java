package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.integrasjon.ldap.LdapBruker;
import no.nav.k9.felles.integrasjon.ldap.LdapBrukeroppslag;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingService;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingVersjon;

@Path("/saksbehandler")
@ApplicationScoped
@Transactional
public class SaksbehandlerRestTjeneste {
    public static final String SAKSBEHANDLER_PATH = "/saksbehandler";
    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(480, TimeUnit.MINUTES);

    private LRUCache<String, String> cache = new LRUCache<>(100, CACHE_ELEMENT_LIVE_TIME_MS);

    private HistorikkRepository historikkRepository;
    private BehandlingRepository behandlingRepository;
    private SykdomVurderingService sykdomVurderingService;

    public SaksbehandlerRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public SaksbehandlerRestTjeneste(HistorikkRepository historikkRepository, BehandlingRepository behandlingRepository, SykdomVurderingService sykdomVurderingService) {
        this.historikkRepository = historikkRepository;
        this.behandlingRepository = behandlingRepository;
        this.sykdomVurderingService = sykdomVurderingService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for ident",
        tags = "nav-ansatt",
        summary = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging.")
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
        List<Historikkinnslag> historikkinnslags = historikkRepository.hentHistorikkForSaksnummer(behandling.getFagsak().getSaksnummer());

        Map<String, String> identer = new HashMap<>();

        historikkinnslags.forEach(historikkinnslag -> {
            sjekkOgLeggTil(identer, historikkinnslag.getOpprettetAv());
            sjekkOgLeggTil(identer, historikkinnslag.getEndretAv());
        });

        LocalDateTimeline<SykdomVurderingVersjon> ktpTimeline = sykdomVurderingService.hentVurderinger(SykdomVurderingType.KONTINUERLIG_TILSYN_OG_PLEIE, behandling);
        ktpTimeline.forEach(i -> sjekkOgLeggTil(identer, i.getValue().getEndretAv()));

        LocalDateTimeline<SykdomVurderingVersjon> toopTimeline = sykdomVurderingService.hentVurderinger(SykdomVurderingType.TO_OMSORGSPERSONER, behandling);
        toopTimeline.forEach(i -> sjekkOgLeggTil(identer, i.getValue().getEndretAv()));

        return new SaksbehandlerDto(identer);
    }

    private void sjekkOgLeggTil(Map<String, String> identer, String ident) {
        if (ident != null) {
            if (!identer.containsKey(ident)) {
                String saksbehandlerCachet = cache.get(ident);
                if (saksbehandlerCachet != null) {
                    identer.put(ident, saksbehandlerCachet);
                } else {
                    LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
                    String brukernavn = ldapBruker.getDisplayName();
                    cache.put(ident, brukernavn);
                }
            }
        }
    }
}
