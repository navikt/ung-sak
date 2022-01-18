package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelseKalkulator;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;

@ApplicationScoped
@Transactional
@Path("/beregning")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningBeregningRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);
    private static final MediaType JSON = MediaType.APPLICATION_JSON_TYPE;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BehandlingRepository behandlingRepository;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    private BeregningsgrunnlagYtelseKalkulator forvaltningBeregning;
    private AksjonspunktRepository aksjonspunktRepository;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private KalkulusRestKlient kalkulusRestKlient;
    private KalkulusRestKlient kalkulusSystemRestKlient;


    public ForvaltningBeregningRestTjeneste() {
    }

    @Inject
    public ForvaltningBeregningRestTjeneste(BeregningsgrunnlagYtelseKalkulator forvaltningBeregning,
                                            BehandlingRepository behandlingRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                            AksjonspunktRepository aksjonspunktRepository,
                                            BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            KalkulusRestKlient kalkulusRestKlient,
                                            SystemUserOidcRestClient systemUserOidcRestClient,
                                            @KonfigVerdi(value = "ftkalkulus.url") URI endpoint) {
        this.forvaltningBeregning = forvaltningBeregning;
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent kalkulatorinput for behandling", tags = "beregning", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer kalkulatorinput på JSON format", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = KalkulatorInputPrVilkårperiodeDto.class)), mediaType = MediaType.APPLICATION_JSON)),
    })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response getKalkulatorInput(@QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) BehandlingIdDto behandlingIdDto) { // NOSONAR
        var behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var mapper = forvaltningBeregning.getYtelsesspesifikkMapper(ref.getFagsakYtelseType());

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        List<KalkulatorInputPrVilkårperiodeDto> inputListe = perioderTilVurdering.stream()
            .filter(periode -> !periodeErUtenforFagsaksIntervall(periode, behandling.getFagsak().getPeriode()))
            .map(vilkårsperiode -> {
                var ytelseGrunnlag = mapper.lagYtelsespesifiktGrunnlag(ref, vilkårsperiode);
                var kalkulatorInput = forvaltningBeregning.getKalkulatorInputTjeneste(ref.getFagsakYtelseType()).byggDto(ref, iayGrunnlag, sakInntektsmeldinger, ytelseGrunnlag, vilkårsperiode, null);
                return new KalkulatorInputPrVilkårperiodeDto(vilkårsperiode, kalkulatorInput);
            })
            .collect(Collectors.toList());

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        cc.setPrivate(true);

        return Response.ok(inputListe, JSON).cacheControl(cc).build();
    }

    @GET
    @Path("inntektsmelding-sortering")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Sorterte inntektsmeldinger per vilkårsperiode", tags = "beregning",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer kalkulatorinput på JSON format", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = Map.class), schema = @Schema(implementation = Inntektsmelding.class)), mediaType = MediaType.APPLICATION_JSON)),
        })
    @BeskyttetRessurs(action = READ, resource = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response inntektsmeldingSortering(@QueryParam("behandlingId") @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) BehandlingIdDto behandlingIdDto) { // NOSONAR
        var behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        Map<DatoIntervallEntitet, Set<Inntektsmelding>> fordeltInntektsmelding = new HashMap<>();

        perioderTilVurdering.forEach(periode -> fordeltInntektsmelding.put(periode,
            sakInntektsmeldinger.stream()
                .filter(it -> it.getOppgittFravær()
                    .stream()
                    .anyMatch(at -> DatoIntervallEntitet.fraOgMedTilOgMed(at.getFom(), at.getTom()).overlapper(periode))
                ).collect(Collectors.toSet())));
        fordeltInntektsmelding.put(behandling.getFagsak().getPeriode(), sakInntektsmeldinger.stream()
            .filter(it -> perioderTilVurdering.stream()
                .noneMatch(at -> it.getOppgittFravær().stream()
                    .noneMatch(of -> DatoIntervallEntitet.fraOgMedTilOgMed(of.getFom(), of.getTom()).overlapper(at))))
            .collect(Collectors.toSet()));

        CacheControl cc = new CacheControl();
        cc.setNoCache(true);
        cc.setNoStore(true);
        cc.setMaxAge(0);
        cc.setPrivate(true);

        return Response.ok(fordeltInntektsmelding, JSON).cacheControl(cc).build();
    }

    @POST
    @Path("migrerAksjonspunkterKalkulus")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Migrerer aksjonspunkt til kalkulus", tags = "beregning")
    @BeskyttetRessurs(action = CREATE, resource = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response migrerAksjonspunkt(@Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtEmptySupplier.class) @Parameter(description = "migrerAksjonspunktDto") no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.MigrerAksjonspunktRequest migrerAksjonspunktDto) { // NOSONAR
        Periode periode = migrerAksjonspunktDto.getPeriode();
        Map<Behandling, Aksjonspunkt> behandlingerMedAksjonspunkt = aksjonspunktRepository.hentAksjonspunkterForKodeUtenVenteÅrsak(periode.getFom(), periode.getTom(), migrerAksjonspunktDto.getAksjonspunktKode());
        List<String> saksummer = behandlingerMedAksjonspunkt.keySet().stream()
            .map(Behandling::getFagsak).map(Fagsak::getSaksnummer)
            .map(Saksnummer::getVerdi)
            .collect(Collectors.toList());
        logger.info("Fant følgende saksnummer med aksjonspunkt " + migrerAksjonspunktDto.getAksjonspunktKode()
            + ": " + saksummer);
        List<MigrerAksjonspunktRequest> aksjonspunktData = behandlingerMedAksjonspunkt.entrySet().stream().map(e -> lagAksjonspunktData(e.getKey(), e.getValue())).collect(Collectors.toList());
        MigrerAksjonspunktListeRequest migrerAksjonspunktListeRequest = new MigrerAksjonspunktListeRequest(aksjonspunktData, migrerAksjonspunktDto.getAksjonspunktKode());
        kalkulusSystemRestKlient.migrerAksjonspunkter(migrerAksjonspunktListeRequest);
        return Response.ok().build();
    }

    private MigrerAksjonspunktRequest lagAksjonspunktData(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        var ref = BehandlingReferanse.fra(behandling);
        var stpTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true).stream()
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toList());
        var bgReferanser = beregningsgrunnlagTjeneste.hentKoblingerForInnvilgedePerioder(ref)
            .stream()
            .filter(kobling -> stpTilVurdering.contains(kobling.getSkjæringstidspunkt()))
            .map(BeregningsgrunnlagKobling::getReferanse)
            .collect(Collectors.toSet());
        return new MigrerAksjonspunktRequest(
            ref.getSaksnummer().getVerdi(),
            YtelseTyperKalkulusStøtterKontrakt.fraKode(ref.getFagsakYtelseType().getKode()),
            bgReferanser,
            aksjonspunkt.getStatus().getKode(),
            aksjonspunkt.getBegrunnelse()
        );
    }


    private boolean periodeErUtenforFagsaksIntervall(DatoIntervallEntitet vilkårPeriode, DatoIntervallEntitet fagsakPeriode) {
        return !vilkårPeriode.overlapper(fagsakPeriode);
    }

}
