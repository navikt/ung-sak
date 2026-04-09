package no.nav.ung.sak.web.app.aktivitetspenger;

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
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.iay.modell.Inntektspost;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.BeregningsgrunnlagDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ytelse.AktivitetspengerUtbetaltMånedDto;
import no.nav.ung.sak.web.app.ungdomsytelse.BehandlingAvsluttetTidspunkt;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.BesteBeregningResultatType;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.PgiÅrsinntektDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;

@Path("")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class AktivitetspengerRestTjeneste {


    public static final String AKTIVITETSPENGER_BASE_PATH = "/aktivitetspenger";
    public static final String BEREGNINGSGRUNNLAG_PATH = AKTIVITETSPENGER_BASE_PATH + "/beregningsgrunnlag";
    public static final String SATS_OG_UTBETALING_PATH = AKTIVITETSPENGER_BASE_PATH + "/månedsvis-sats-og-utbetaling";

    private BehandlingRepository behandlingRepository;
    private AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private BeregningTjeneste beregningTjeneste;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;

    public AktivitetspengerRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerRestTjeneste(BehandlingRepository behandlingRepository,
                                        AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
                                        BeregningTjeneste beregningTjeneste,
                                        TilkjentYtelseRepository tilkjentYtelseRepository,
                                        MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
        this.beregningTjeneste = beregningTjeneste;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
    }

    @GET
    @Operation(description = "Henter månedsvis satser og utbetaling", tags = "avp")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(SATS_OG_UTBETALING_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<AktivitetspengerUtbetaltMånedDto> getSatsOgUtbetalingPerioder(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var grunnlagOpt = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId());
        if (grunnlagOpt.isEmpty() || grunnlagOpt.get().getSatsperioder() == null || grunnlagOpt.get().getBeregningsgrunnlag().isEmpty()) {
            return Collections.emptyList();
        }
        var grunnlag = grunnlagOpt.get();
        var satsTidslinje = grunnlag.hentAktivitetspengerSatsTidslinje();
        var månedsvisPeriodisering = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(behandling.getId());
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId());
        var kontrollertInntektTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandling.getId());
        var tidslinjeMap = tilkjentYtelseRepository.hentTidslinjerForFagsak(behandling.getFagsakId());
        var avsluttetTidTilkjentYtelseMap = tidslinjeMap.entrySet().stream()
            .collect(Collectors.toMap(e -> BehandlingAvsluttetTidspunkt.fraBehandling(e.getKey()), Map.Entry::getValue));

        return MånedsvisningDtoMapper.mapSatsOgUtbetalingPrMåned(
            BehandlingAvsluttetTidspunkt.fraBehandling(behandling),
            månedsvisPeriodisering,
            tilkjentYtelseTidslinje,
            kontrollertInntektTidslinje,
            satsTidslinje,
            avsluttetTidTilkjentYtelseMap);
    }

    @GET
    @Operation(description = "Henter beregningsgrunnlag for en aktivitetspengerbehandling", tags = "avp")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(BEREGNINGSGRUNNLAG_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsgrunnlagDto getBeregningsgrunnlag(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        var inntektsposter = beregningTjeneste.hentSigrunInntektsposter(behandling.getId());

        return aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId())
            .flatMap(AktivitetspengerGrunnlag::getSenesteBeregningsgrunnlag)
            .map(grunnlag -> mapTilBeregningsgrunnlagDto(grunnlag, inntektsposter))
            .orElseThrow(() -> new IllegalStateException("Fant ikke beregningsgrunnlag for behandlingid: " + behandling.getId()));
    }

    private static BeregningsgrunnlagDto mapTilBeregningsgrunnlagDto(Beregningsgrunnlag grunnlag, List<Inntektspost> inntektsposter) {
        BeregningInput beregningInput = grunnlag.getBeregningInput().getBeregningInput(grunnlag.getSkjæringstidspunkt());
        PgiKalkulator pgiKalkulator = new PgiKalkulator(beregningInput);
        Map<Year, BigDecimal> sumAvkortetPerÅr = pgiKalkulator.avgrensÅrsinntekterUtenOppjustering();
        Map<Year, BigDecimal> sumAvkortetOgOppjustertPerÅr = pgiKalkulator.avgrensOgOppjusterÅrsinntekter();
        PgiHjelper pgiHjelper = new PgiHjelper(inntektsposter, beregningInput.sisteLignedeÅr());

        List<PgiÅrsinntektDto> pgiÅrsinntekter = beregningInput.lagTidslinje().toSegments().stream()
            .map(segment -> {
                Year år = Year.of(segment.getFom().getYear());

                var pgiInntektstyper = pgiHjelper.hentSumPrInntektspostTypePrÅr().getOrDefault(år, Map.of());
                BigDecimal arbeidsinntekt = hentPgiForTyper(pgiInntektstyper, Set.of(InntektspostType.LØNN));
                BigDecimal næring = hentPgiForTyper(pgiInntektstyper, Set.of(InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, InntektspostType.NÆRING_FISKE_FANGST_FAMBARNEHAGE));

                return new PgiÅrsinntektDto(
                    år.getValue(),
                    segment.getValue().getVerdi().setScale(0, RoundingMode.HALF_UP),
                    sumAvkortetPerÅr.getOrDefault(år, BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP),
                    sumAvkortetOgOppjustertPerÅr.getOrDefault(år, BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP),
                    arbeidsinntekt.setScale(0, RoundingMode.HALF_UP),
                    næring.setScale(0, RoundingMode.HALF_UP)
                );
            })
            .sorted(Comparator.comparingInt(PgiÅrsinntektDto::årstall).reversed())
            .toList();

        return new BeregningsgrunnlagDto(
            grunnlag.getSkjæringstidspunkt(),
            grunnlag.getÅrsinntektAvkortetOppjustertSisteÅr().setScale(0, RoundingMode.HALF_UP),
            grunnlag.getÅrsinntektAvkortetOppjustertSisteTreÅr().setScale(0, RoundingMode.HALF_UP),
            grunnlag.getBeregnetPrAar().setScale(0, RoundingMode.HALF_UP),
            grunnlag.getBeregnetRedusertPrAar().setScale(0, RoundingMode.HALF_UP),
            grunnlag.getDagsats().setScale(0, RoundingMode.HALF_UP).intValueExact(),
            pgiÅrsinntekter,
            mapBesteBeregningResultatType(grunnlag.utledBesteBeregningResultatType())
        );
    }

    private static BigDecimal hentPgiForTyper(Map<InntektspostType, Beløp> pgiInntektstyper, Set<InntektspostType> typer) {
        return typer.stream()
            .map(type -> pgiInntektstyper.getOrDefault(type, Beløp.ZERO).getVerdi())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BesteBeregningResultatType mapBesteBeregningResultatType(no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BesteBeregningResultatType type) {
        return switch (type) {
            case SISTE_ÅR -> BesteBeregningResultatType.SISTE_ÅR;
            case SNITT_SISTE_TRE_ÅR -> BesteBeregningResultatType.SNITT_SISTE_TRE_ÅR;
        };
    }
}
