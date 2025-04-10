package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringRapportertInntektDto;
import no.nav.ung.sak.formidling.template.dto.endring.inntekt.EndringRapportertInntektPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

@Dependent
public class EndringRapportertInntektInnholdBygger implements VedtaksbrevInnholdBygger {

    private final TilkjentYtelseRepository tilkjentYtelseRepository;
    private final RapportertInntektMapper rapportertInntektMapper;

    //TODO hente fra et annet sted?
    public static final BigDecimal REDUKSJONS_FAKTOR = BigDecimal.valueOf(0.66);
    private static final int REDUSJON_PROSENT = REDUKSJONS_FAKTOR.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    private static final Logger LOG = LoggerFactory.getLogger(EndringRapportertInntektInnholdBygger.class);

    @Inject
    public EndringRapportertInntektInnholdBygger(
        TilkjentYtelseRepository tilkjentYtelseRepository,
        RapportertInntektMapper rapportertInntektMapper) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.rapportertInntektMapper = rapportertInntektMapper;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        var relevantTilkjentYtelse = resultatTidslinje.combine(tilkjentYtelseTidslinje, StandardCombinators::rightOnly,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);

        if (relevantTilkjentYtelse.isEmpty()) {
            throw new IllegalStateException("Fant ingen tilkjent ytelse i perioden" + resultatTidslinje.getLocalDateIntervals());
        }

        var rapporteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandling.getId());

        var periodeDtoTidslinje = relevantTilkjentYtelse.combine(rapporteInntekterTidslinje,
            EndringRapportertInntektInnholdBygger::mapTilPeriodeDto,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);

        EndringRapportertInntektDto dto = aggregerOgLagDto(periodeDtoTidslinje);

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_INNTEKT, dto);
    }

    private EndringRapportertInntektDto aggregerOgLagDto(
        LocalDateTimeline<EndringRapportertInntektPeriodeDto> periodeDtoTidslinje) {
        long sumRapportertInntekt = periodeDtoTidslinje.toSegments().stream()
            .map(it -> it.getValue().rapportertInntekt())
            .reduce(0L, Long::sum);

        long sumUtbetaling = periodeDtoTidslinje.toSegments().stream()
            .map(it -> it.getValue().utbetalingBeløp())
            .reduce(0L, Long::sum);

        return new EndringRapportertInntektDto(
            new PeriodeDto(periodeDtoTidslinje.getMinLocalDate(), periodeDtoTidslinje.getMaxLocalDate()),
            sumRapportertInntekt,
            sumUtbetaling,
            REDUSJON_PROSENT,
            periodeDtoTidslinje.size() > 1,
            periodeDtoTidslinje.toSegments().stream()
                .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval))
                .map(LocalDateSegment::getValue)
                .collect(Collectors.toList())
        );

    }

    private static LocalDateSegment<EndringRapportertInntektPeriodeDto> mapTilPeriodeDto(
        LocalDateInterval p, LocalDateSegment<TilkjentYtelseVerdi> lhs, LocalDateSegment<RapporterteInntekter> rhs) {
        var ty = lhs.getValue();

        Objects.requireNonNull(rhs, "Mangler sats og rapportert inntekt for periode %s for tilkjent ytelse %s"
            .formatted(p.toString(), ty.toString()));

        var rapportertInntektSum = rhs.getValue().brukerRapporterteInntekter().stream()
            .map(RapportertInntekt::beløp).reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LocalDateSegment<>(p,
            new EndringRapportertInntektPeriodeDto(
                new PeriodeDto(p.getFomDato(), p.getTomDato()),
                rapportertInntektSum.setScale(0, RoundingMode.HALF_UP).longValue(),
                ty.redusertBeløp().setScale(0, RoundingMode.HALF_UP).longValue(),
                REDUSJON_PROSENT
            )
        );
    }

}
