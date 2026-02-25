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
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPerioder;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.UngdomsprogramInformasjonDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.beregning.UngdomsytelseSatsPeriodeDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.uttak.UngdomsytelseUttakPeriodeDto;
import no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse.UngdomsytelseUtbetaltMånedDto;
import no.nav.ung.sak.tid.Virkedager;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;
import no.nav.ung.sak.web.app.ungdomsytelse.BehandlingAvsluttetTidspunkt;
import no.nav.ung.sak.web.app.ungdomsytelse.MånedsvisningDtoMapper;
import no.nav.ung.sak.web.server.abac.AbacAttributtSupplier;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType.READ;
import static no.nav.ung.sak.tid.AbstractLocalDateInterval.TIDENES_ENDE;

@Path("")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class AktivitetspengerRestTjeneste {


    public static final String AKTIVITETSPENGER_BASE_PATH = "/aktivitetspenger";
    public static final String SATSER_PATH = AKTIVITETSPENGER_BASE_PATH + "/satser";
    public static final String MÅNEDSVIS_SATS_OG_UTBETALING_PATH = AKTIVITETSPENGER_BASE_PATH + "/månedsvis-sats-og-utbetaling";

    public static final String UTTAK_PATH = AKTIVITETSPENGER_BASE_PATH + "/uttak";

    private BehandlingRepository behandlingRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;

    public AktivitetspengerRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public AktivitetspengerRestTjeneste(BehandlingRepository behandlingRepository,
                                        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
                                        TilkjentYtelseRepository tilkjentYtelseRepository, MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
    }

    @GET
    @Operation(description = "Henter innvilgede satser for en aktivitetspengerbehandling", tags = "ung")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(SATSER_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<UngdomsytelseSatsPeriodeDto> getAktivitetspengerInnvilgetSats(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Optional<UngdomsytelseGrunnlag> grunnlag = hentUngdomsytelseGrunnlag(behandlingUuid);
        UngdomsytelseSatsPerioder perioder = grunnlag.map(UngdomsytelseGrunnlag::getSatsPerioder).orElse(null);
        if (perioder == null) {
            return Collections.emptyList();
        } else {
            return mapSatsperioder(perioder);
        }
    }

    @GET
    @Operation(description = "Henter månedsvis satser og utbetaling", tags = "ung")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(MÅNEDSVIS_SATS_OG_UTBETALING_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<UngdomsytelseUtbetaltMånedDto> getSatsOgUtbetalingPerioder(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Optional<UngdomsytelseGrunnlag> grunnlag = hentUngdomsytelseGrunnlag(behandlingUuid);
        UngdomsytelseSatsPerioder perioder = grunnlag.map(UngdomsytelseGrunnlag::getSatsPerioder).orElse(null);
        if (perioder == null) {
            return Collections.emptyList();
        }
        final var behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        final var månedsvisPeriodisering = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(behandling.getId());
        final var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId());
        final var kontrollertInntektTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandling.getId());
        var tidslinjeMap = tilkjentYtelseRepository.hentTidslinjerForFagsak(behandling.getFagsakId());
        var avsluttetTidTilkjentYtelseMap = tidslinjeMap.entrySet().stream().collect(Collectors.toMap(e -> BehandlingAvsluttetTidspunkt.fraBehandling(e.getKey()), Map.Entry::getValue));
        return MånedsvisningDtoMapper.mapSatsOgUtbetalingPrMåned(
            BehandlingAvsluttetTidspunkt.fraBehandling(behandling),
            månedsvisPeriodisering,
            tilkjentYtelseTidslinje,
            kontrollertInntektTidslinje,
            perioder,
            avsluttetTidTilkjentYtelseMap);
    }

    @GET
    @Operation(description = "Henter uttaksperioder for en aktivitetspengerbehandling", tags = "ung")
    @BeskyttetRessurs(action = READ, resource = BeskyttetRessursResourceType.FAGSAK)
    @Path(UTTAK_PATH)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<UngdomsytelseUttakPeriodeDto> getUngdomsytelseUttak(@NotNull @QueryParam(BehandlingUuidDto.NAME) @Parameter(description = BehandlingUuidDto.DESC) @Valid @TilpassetAbacAttributt(supplierClass = AbacAttributtSupplier.class) BehandlingUuidDto behandlingUuid) {
        Optional<UngdomsytelseGrunnlag> grunnlag = hentUngdomsytelseGrunnlag(behandlingUuid);
        UngdomsytelseUttakPerioder uttakPerioder = grunnlag.map(UngdomsytelseGrunnlag::getUttakPerioder).orElse(null);
        if (uttakPerioder == null) {
            return Collections.emptyList();
        } else {
            return uttakPerioder.getPerioder().stream()
                .map(p -> new UngdomsytelseUttakPeriodeDto(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.getAvslagsårsak()))
                .toList();
        }
    }

    private static List<UngdomsytelseSatsPeriodeDto> mapSatsperioder(UngdomsytelseSatsPerioder perioder) {
        return perioder.getPerioder().stream()
            .map(AktivitetspengerRestTjeneste::mapTilSatsperiode)
            .toList();
    }

    private static UngdomsytelseSatsPeriodeDto mapTilSatsperiode(UngdomsytelseSatsPeriode p) {
        return new UngdomsytelseSatsPeriodeDto(
            p.getPeriode().getFomDato(),
            p.getPeriode().getTomDato(),
            p.getDagsats(),
            p.getGrunnbeløpFaktor(),
            p.getGrunnbeløp(),
            p.getSatsType(),
            p.getAntallBarn(),
            p.getDagsatsBarnetillegg(),
            Virkedager.beregnAntallVirkedager(p.getPeriode().getFomDato(), p.getPeriode().getTomDato()));
    }

    private Optional<UngdomsytelseGrunnlag> hentUngdomsytelseGrunnlag(BehandlingUuidDto behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid.getBehandlingUuid());
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.UNGDOMSYTELSE) {
            throw new IllegalArgumentException("Tjenesten virker kun for ungdomsytelse, fikk behandling for annen ytelse");
        }
        Optional<UngdomsytelseGrunnlag> grunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId());
        return grunnlag;
    }


}
