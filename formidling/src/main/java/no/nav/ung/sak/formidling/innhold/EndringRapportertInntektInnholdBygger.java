package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.formidling.template.dto.EndringRapportertInntektDto;
import no.nav.ung.sak.formidling.template.dto.endring.inntekt.EndringRapportertInntektPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

import static no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger.tilHeltall;

@Dependent
public class EndringRapportertInntektInnholdBygger implements VedtaksbrevInnholdBygger {

    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    //TODO hente fra et annet sted?
    public static final BigDecimal REDUKSJONS_FAKTOR = BigDecimal.valueOf(0.66);
    private static final int REDUSJON_PROSENT = REDUKSJONS_FAKTOR.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();

    @Inject
    public EndringRapportertInntektInnholdBygger(
        TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();
        final var kontrollertInntektPerioderTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(behandling.getId());

        var relevantTilkjentYtelse = DetaljertResultat
            .filtererTidslinje(resultatTidslinje, DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON)
            .combine(tilkjentYtelseTidslinje, StandardCombinators::rightOnly,
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        if (relevantTilkjentYtelse.isEmpty()) {
            throw new IllegalStateException("Fant ingen tilkjent ytelse i perioden" + resultatTidslinje.getLocalDateIntervals());
        }

        var periodeDtoTidslinje = relevantTilkjentYtelse.combine(kontrollertInntektPerioderTidslinje,
            EndringRapportertInntektInnholdBygger::mapTilPeriodeDto,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);

        EndringRapportertInntektDto dto = new EndringRapportertInntektDto(
            REDUSJON_PROSENT,
            periodeDtoTidslinje.toSegments().stream()
                .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval))
                .map(LocalDateSegment::getValue)
                .collect(Collectors.toList())
        );

        return new TemplateInnholdResultat(TemplateType.ENDRING_INNTEKT, dto, true);
    }

    private static LocalDateSegment<EndringRapportertInntektPeriodeDto> mapTilPeriodeDto(
        LocalDateInterval p, LocalDateSegment<TilkjentYtelseVerdi> lhs, LocalDateSegment<BigDecimal> rhs) {
        var ty = lhs.getValue();

        Objects.requireNonNull(rhs, "Mangler kontrollert inntekt for periode %s for tilkjent ytelse %s"
            .formatted(p.toString(), ty.toString()));

        return new LocalDateSegment<>(p,
            new EndringRapportertInntektPeriodeDto(
                new PeriodeDto(p.getFomDato(), p.getTomDato()),
                tilHeltall(rhs.getValue()),
                tilHeltall(ty.tilkjentBeløp()),
                REDUSJON_PROSENT
            )
        );
    }

}
