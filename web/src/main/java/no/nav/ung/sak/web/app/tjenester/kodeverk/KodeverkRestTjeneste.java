package no.nav.ung.sak.web.app.tjenester.kodeverk;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.web.app.jackson.ObjectMapperFactory;
import no.nav.ung.sak.web.app.tjenester.kodeverk.dto.AlleKodeverdierSomObjektResponse;
import no.nav.ung.sak.web.app.tjenester.kodeverk.dto.KodeverdiSomObjekt;
import no.nav.ung.sak.web.app.tjenester.kodeverk.dto.LegacyVenteårsakSomObjekt;
import no.nav.ung.sak.web.app.tjenester.kodeverk.dto.VenteårsakSomObjekt;
import no.nav.ung.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.ung.sak.web.server.caching.CacheControl;

@Path("/kodeverk")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class KodeverkRestTjeneste {

    public static final String KODERVERK_PATH = "/kodeverk";
    public static final String ENHETER_PATH = KODERVERK_PATH + "/behandlende-enheter";

    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    @Inject
    public KodeverkRestTjeneste(BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    public KodeverkRestTjeneste() {
        // for resteasy
    }


    @GET
    @Path("/ung-sak/kodeverk/typer")
    @Operation(description = "Ikkje reell implementasjon for bruk. Kun for openapi type generering av ung-sak kodeverkstyper", tags = "kodeverk")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    public KodeverkWeb getUngSakKodeverkTyper() {
        return new KodeverkWeb();
    }

    @GET
    @Path("/behandlende-enheter")
    @Operation(description = "Henter liste over behandlende enheter", tags = "kodeverk")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<OrganisasjonsEnhet> hentBehandlendeEnheter(@QueryParam("ytelseType") @DefaultValue(value = "OMP") @NotNull @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) FagsakYtelseType ytelseType) {
        return behandlendeEnhetTjeneste.hentEnhetListe(ytelseType);
    }

    private final static AlleKodeverdierSomObjektResponse oppslagAlleResponse;

    private static <KV extends Kodeverdi> SortedSet<KodeverdiSomObjekt<KV>> sortert(Set<KV> verdier) {
        return KodeverdiSomObjekt.sorterte(verdier);
    }

    static {
        final var alle = StatiskeKodeverdier.alle;

        final Map<String, SortedSet<KodeverdiSomObjekt<Avslagsårsak>>> avslagårsakerGruppertPåVilkårType =
            VilkårType.finnAvslagårsakerGruppertPåVilkårType().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().getKode(), e -> sortert(e.getValue())));

        oppslagAlleResponse = new AlleKodeverdierSomObjektResponse(
            sortert(alle.relatertYtelseTilstander()),
            sortert(alle.fagsakStatuser()),
            sortert(alle.fagsakYtelseTyper()),
            sortert(alle.behandlingÅrsakTyper()),
            sortert(alle.historikkBegrunnelseTyper()),
            sortert(alle.oppgaveÅrsaker()),
            sortert(alle.medlemskapManuellVurderingTyper()),
            sortert(alle.behandlingResultatTyper()),
            sortert(alle.personstatusTyper()),
            VenteårsakSomObjekt.sorterteVenteårsaker(alle.venteårsaker()),
            sortert(alle.behandlingTyper()),
            sortert(alle.arbeidTyper()),
            sortert(alle.opptjeningAktivitetTyper()),
            sortert(alle.revurderingVarslingÅrsaker()),
            sortert(alle.inntektskategorier()),
            sortert(alle.aktivitetStatuser()),
            sortert(alle.arbeidskategorier()),
            sortert(alle.fagsystemer()),
            sortert(alle.sivilstandTyper()),
            sortert(alle.faktaOmBeregningTilfeller()),
            sortert(alle.skjermlenkeTyper()),
            sortert(alle.historikkOpplysningTyper()),
            sortert(alle.historikkEndretFeltTyper()),
            sortert(alle.historikkEndretFeltVerdiTyper()),
            sortert(alle.historikkinnslagTyper()),
            sortert(alle.historikkAktører()),
            sortert(alle.historikkAvklartSoeknadsperiodeTyper()),
            sortert(alle.historikkResultatTyper()),
            sortert(alle.behandlingStatuser()),
            sortert(alle.medlemskapDekningTyper()),
            sortert(alle.medlemskapTyper()),
            sortert(alle.avslagsårsaker()),
            sortert(alle.konsekvenserForYtelsen()),
            sortert(alle.vilkårTyper()),
            sortert(alle.vurderArbeidsforholdHistorikkinnslag()),
            sortert(alle.tilbakekrevingVidereBehandlinger()),
            sortert(alle.vurderingsÅrsaker()),
            sortert(alle.regioner()),
            sortert(alle.landkoder()),
            sortert(alle.språkkoder()),
            sortert(alle.vedtakResultatTyper()),
            sortert(alle.dokumentTypeIder()),
            sortert(alle.årsakerTilVurdering()),
            new TreeMap<>(avslagårsakerGruppertPåVilkårType)
        );
    }

    @GET
    @Path("/alle/objekt")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    @Operation(description = "Alle statisk kodeverdier som objekt", tags = "kodeverk")
    @CacheControl(maxAge = 60)
    public AlleKodeverdierSomObjektResponse alleKodeverdierSomObjekt() {
        return KodeverkRestTjeneste.oppslagAlleResponse;
    }

    // Samme som KodeverdiOppslagResponse, men med klassenamn som property namn. For at gammalt endepunkt skal svare med samme respons som det har gjort.
    private final static Map<String, Object> legacyGruppertKodeverdi;

    private static <V extends Kodeverdi> Map.Entry<String, SortedSet<KodeverdiSomObjekt<V>>> toLegacyGruppertKodeverdi(SortedSet<KodeverdiSomObjekt<V>> objekter) {
        return new AbstractMap.SimpleEntry<>(objekter.first().madeFromClassName(), objekter);
    }

    static {
        final var o = oppslagAlleResponse;
        final List<SortedSet<? extends KodeverdiSomObjekt<?>>> alleKodeverdier;
        alleKodeverdier = Arrays.asList(
            o.relatertYtelseTilstander(),
            o.fagsakStatuser(),
            o.fagsakYtelseTyper(),
            o.behandlingÅrsakTyper(),
            o.historikkBegrunnelseTyper(),
            o.oppgaveÅrsaker(),
            o.medlemskapManuellVurderingTyper(),
            o.behandlingResultatTyper(),
            o.personstatusTyper(),
            o.venteårsaker(),
            o.behandlingTyper(),
            o.arbeidTyper(),
            o.opptjeningAktivitetTyper(),
            o.revurderingVarslingÅrsaker(),
            o.inntektskategorier(),
            o.aktivitetStatuser(),
            o.arbeidskategorier(),
            o.fagsystemer(),
            o.sivilstandTyper(),
            o.faktaOmBeregningTilfeller(),
            o.skjermlenkeTyper(),
            o.historikkOpplysningTyper(),
            o.historikkEndretFeltTyper(),
            o.historikkEndretFeltVerdiTyper(),
            o.historikkinnslagTyper(),
            o.historikkAktører(),
            o.historikkAvklartSoeknadsperiodeTyper(),
            o.historikkResultatTyper(),
            o.behandlingStatuser(),
            o.medlemskapDekningTyper(),
            o.medlemskapTyper(),
            o.avslagsårsaker(),
            o.konsekvenserForYtelsen(),
            o.vilkårTyper(),
            o.vurderArbeidsforholdHistorikkinnslag(),
            o.tilbakekrevingVidereBehandlinger(),
            o.vurderingsÅrsaker(),
            o.regioner(),
            o.landkoder(),
            o.språkkoder(),
            o.vedtakResultatTyper(),
            o.dokumentTypeIder(),
            o.årsakerTilVurdering()
        );

        final Map<String, Object> r = new LinkedHashMap<>();
        for (final var kodeverdierForType : alleKodeverdier) {
            final var tilpassaLegacyEndepunkt = kodeverdierForType.stream()
                .filter(kv -> kv != null && !"-".equals(kv.getKode())) // Gammalt endepunkt returnerer ikkje "-" verdiane
                .map(kv -> {
                    if (kv instanceof VenteårsakSomObjekt) {
                        return new LegacyVenteårsakSomObjekt(((VenteårsakSomObjekt) kv).getMadeFrom());
                    } else {
                        return kv;
                    }
                })
                .collect(Collectors.toSet());
            r.put(kodeverdierForType.getFirst().madeFromClassName(), tilpassaLegacyEndepunkt);
        }
        // Avslagsårsak blir lagt til som eit spesialtilfelle, gruppert pr vilkårtype.
        r.put(Avslagsårsak.class.getSimpleName(), o.avslagårsakerPrVilkårTypeKode());
        legacyGruppertKodeverdi = r;
    }

    /**
     * @deprecated Bruk {@link #alleKodeverdierSomObjekt()} istaden.
     */
    @GET
    @Operation(description = "Henter kodeliste", deprecated = true, tags = "kodeverk")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    @Deprecated(forRemoval = true)
    public Response hentGruppertKodeliste() throws IOException {
        final ObjectMapper om = ObjectMapperFactory.createBaseObjectMapper();

        String kodelisteJson = om.writeValueAsString(legacyGruppertKodeverdi);
        jakarta.ws.rs.core.CacheControl cc = new jakarta.ws.rs.core.CacheControl();
        cc.setMaxAge(1 * 60); // tillater klient caching i 1 minutt
        return Response.ok()
            .entity(kodelisteJson)
            .type(MediaType.APPLICATION_JSON)
            .cacheControl(cc)
            .build();

    }

}
