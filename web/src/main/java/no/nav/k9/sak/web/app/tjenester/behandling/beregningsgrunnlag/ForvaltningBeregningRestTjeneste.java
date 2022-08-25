package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import static no.nav.k9.abac.BeskyttetRessursKoder.DRIFT;
import static no.nav.k9.abac.BeskyttetRessursKoder.FAGSAK;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;
import no.nav.folketrygdloven.beregningsgrunnlag.forvaltning.GjenopprettUgyldigeReferanserForBehandlingTask;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelseKalkulator;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagKobling;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.migrerAksjonspunkt.MigrerAksjonspunktRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.AbacDto;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.FagsakTjeneste;
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
import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningMidlertidigDriftRestTjeneste;
import no.nav.k9.sak.web.server.abac.AbacAttributtEmptySupplier;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPerioderGrunnlag;

@ApplicationScoped
@Transactional
@Path("/beregning")
@Produces(MediaType.APPLICATION_JSON)
public class ForvaltningBeregningRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);
    private static final MediaType JSON = MediaType.APPLICATION_JSON_TYPE;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskRepository;
    private FagsakTjeneste fagsakTjeneste;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    private BeregningsgrunnlagYtelseKalkulator forvaltningBeregning;
    private AksjonspunktRepository aksjonspunktRepository;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private KalkulusRestKlient kalkulusRestKlient;
    private KalkulusRestKlient kalkulusSystemRestKlient;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;


    public ForvaltningBeregningRestTjeneste() {
    }

    @Inject
    public ForvaltningBeregningRestTjeneste(BeregningsgrunnlagYtelseKalkulator forvaltningBeregning,
                                            BehandlingRepository behandlingRepository,
                                            ProsessTaskTjeneste prosessTaskRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                            FagsakTjeneste fagsakTjeneste, AksjonspunktRepository aksjonspunktRepository,
                                            BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                            KalkulusRestKlient kalkulusRestKlient,
                                            SystemUserOidcRestClient systemUserOidcRestClient,
                                            @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste, BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository) {
        this.forvaltningBeregning = forvaltningBeregning;
        this.behandlingRepository = behandlingRepository;
        this.prosessTaskRepository = prosessTaskRepository;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.aksjonspunktRepository = aksjonspunktRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
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

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false);

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


    @POST
    @Path("/gjenopprett-referanser-feil")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gjenoppretter referanser der revurdering har endret initiell versjon.",
        summary = ("Gjenoppretter referanser der revurdering har endret initiell versjon."),
        tags = "beregning",
        responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer saksnummer",
                content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = SaksnummerDto.class)), mediaType = MediaType.APPLICATION_JSON))
        })
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, resource = DRIFT)
    public Response finnFeilVedGjenoppretting(@Parameter(description = "Saksnumre (skilt med mellomrom eller linjeskift)") @Valid Saksliste saksliste) {
        var alleSaksnummer = Objects.requireNonNull(saksliste.getSaksnumre(), "saksnumre");
        var saknumre = new LinkedHashSet<>(Arrays.asList(alleSaksnummer.split("\\s+")));

        var ferdigeGjenopprettinger = prosessTaskRepository.finnAlle(GjenopprettUgyldigeReferanserForBehandlingTask.TASKTYPE, ProsessTaskStatus.FERDIG);

        var result = new ArrayList<SaksnummerDto>();

        for (var s : saknumre) {
            var fagsak = fagsakTjeneste.finnFagsakGittSaksnummer(new Saksnummer(s), false).orElseThrow(() -> new IllegalArgumentException("finnes ikke fagsak med saksnummer: " + s));

            var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(this.vilkårsPerioderTilVurderingTjeneste, fagsak.getYtelseType(), BehandlingType.REVURDERING);

            var sisteRevurdering = behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(fagsak.getId(), BehandlingType.REVURDERING);

            if (sisteRevurdering.isPresent() && ferdigeGjenopprettinger.stream().noneMatch(t -> sisteRevurdering.get().getId().equals(Long.valueOf(t.getBehandlingId())))) {
                var ikkeVurdert = finnIkkeVurdertePerioder(perioderTilVurderingTjeneste, sisteRevurdering);
                var originalPerioderTilVurdering = finnVurdertePerioderIOriginalBehandling(perioderTilVurderingTjeneste, sisteRevurdering);
                var ikkeVurdertIRevurderingMenVurdertIOriginal = ikkeVurdert.stream().filter(originalPerioderTilVurdering::contains).toList();
                var initiellVersjonRevurdering = beregningPerioderGrunnlagRepository.getInitiellVersjon(sisteRevurdering.get().getId());
                var uvurderteInitiellePerioder = initiellVersjonRevurdering.stream().flatMap(it -> it.getGrunnlagPerioder().stream())
                    .filter(p -> ikkeVurdertIRevurderingMenVurdertIOriginal.stream().anyMatch(it -> it.getFomDato().equals(p.getSkjæringstidspunkt())))
                    .toList();
                var originaltGrunnlag = beregningPerioderGrunnlagRepository.hentGrunnlag(sisteRevurdering.get().getId());
                boolean harUgyldigInitiellReferanse = harUgyldigInitiellReferanse(uvurderteInitiellePerioder, originaltGrunnlag);


                if (harUgyldigInitiellReferanse) {
                    result.add(new SaksnummerDto(new Saksnummer(s)));
                }
            }
        }

        return Response.ok(result).build();

    }

    private boolean harUgyldigInitiellReferanse(List<BeregningsgrunnlagPeriode> uvurderteInitiellePerioder, Optional<BeregningsgrunnlagPerioderGrunnlag> originaltGrunnlag) {
        return originaltGrunnlag.stream()
            .flatMap(gr -> gr.getGrunnlagPerioder().stream())
            .anyMatch(it -> !finnInitiellReferanse(uvurderteInitiellePerioder, it).equals(it.getEksternReferanse()));
    }

    private NavigableSet<DatoIntervallEntitet> finnVurdertePerioderIOriginalBehandling(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, Optional<Behandling> sisteRevurdering) {
        var originalBehandlingId = sisteRevurdering.get().getOriginalBehandlingId().orElseThrow();
        return perioderTilVurderingTjeneste.utled(originalBehandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
    }

    private List<DatoIntervallEntitet> finnIkkeVurdertePerioder(VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste, Optional<Behandling> sisteRevurdering) {
        var perioderTilVurdering = perioderTilVurderingTjeneste.utled(sisteRevurdering.get().getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var fullstendigPeriode = perioderTilVurderingTjeneste.utledFullstendigePerioder(sisteRevurdering.get().getId());
        return fullstendigPeriode.stream().filter(p -> !perioderTilVurdering.contains(p)).toList();
    }

    private UUID finnInitiellReferanse(List<BeregningsgrunnlagPeriode> uvurderteInitiellePerioder, BeregningsgrunnlagPeriode it) {
        return uvurderteInitiellePerioder.stream()
            .filter(it2 -> it2.getSkjæringstidspunkt().equals(it.getSkjæringstidspunkt()))
            .findFirst().orElseThrow().getEksternReferanse();
    }

    private MigrerAksjonspunktRequest lagAksjonspunktData(Behandling behandling, Aksjonspunkt aksjonspunkt) {
        var ref = BehandlingReferanse.fra(behandling);
        var stpTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true).stream()
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toList());
        var bgReferanser = beregningsgrunnlagTjeneste.hentKoblingerForPerioder(ref)
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


    public static class Saksliste implements AbacDto {

        @NotNull
        @Pattern(regexp = "^[\\p{Alnum}\\s]+$", message = "OpprettManuellRevurdering [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
        private String saksnumre;

        public Saksliste() {
            // empty ctor
        }

        public Saksliste(@NotNull String saksnumre) {
            this.saksnumre = saksnumre;
        }

        @NotNull
        public String getSaksnumre() {
            return saksnumre;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }

        @Provider
        public static class SakslisteMessageBodyReader implements MessageBodyReader<Saksliste> {

            @Override
            public boolean isReadable(Class<?> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType) {
                return (type == ForvaltningMidlertidigDriftRestTjeneste.OpprettManuellRevurdering.class);
            }

            @Override
            public Saksliste readFrom(Class<Saksliste> type, Type genericType,
                                      Annotation[] annotations, MediaType mediaType,
                                      MultivaluedMap<String, String> httpHeaders,
                                      InputStream inputStream)
                throws IOException, WebApplicationException {
                var sb = new StringBuilder(200);
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(inputStream))) {
                    sb.append(br.readLine()).append('\n');
                }

                return new Saksliste(sb.toString());

            }
        }
    }

}
