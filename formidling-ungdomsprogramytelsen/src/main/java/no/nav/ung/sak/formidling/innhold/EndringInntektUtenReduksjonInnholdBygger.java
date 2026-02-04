package no.nav.ung.sak.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.formidling.template.dto.EndringInntektUtenReduksjonDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Collectors;

@Dependent
public class EndringInntektUtenReduksjonInnholdBygger implements VedtaksbrevInnholdBygger {

    private final TilkjentYtelseRepository tilkjentYtelseRepository;

    @Inject
    public EndringInntektUtenReduksjonInnholdBygger(
        TilkjentYtelseRepository tilkjentYtelseRepository) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        var relevantTilkjentYtelse = DetaljertResultat
            .filtererTidslinje(resultatTidslinje, DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING)
            .combine(tilkjentYtelseTidslinje, StandardCombinators::rightOnly,
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        if (relevantTilkjentYtelse.isEmpty()) {
            throw new IllegalStateException("Fant ingen tilkjent ytelse i perioden" + resultatTidslinje.getLocalDateIntervals());
        }

        if (relevantTilkjentYtelse.stream().anyMatch(it -> it.getValue().utbetalingsgrad().compareTo(BigDecimal.valueOf(100)) < 0)) {
            throw new IllegalStateException("Fant tilkjent ytelse med utbetalingsgrad mindre enn 100%.");
        }

        var fullUtbetalingsperioder = relevantTilkjentYtelse.mapValue(_ -> true)
            .compress()
            .toSegments().stream()
            .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval))
            .map(it -> new PeriodeDto(it.getFom(), it.getTom()))
            .collect(Collectors.toSet());

        return new TemplateInnholdResultat(TemplateType.ENDRING_INNTEKT_UTEN_REDUKSJON,
            new EndringInntektUtenReduksjonDto(fullUtbetalingsperioder)
        );
    }

}
