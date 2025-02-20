package no.nav.ung.sak.formidling.innhold;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringDto;
import no.nav.ung.sak.formidling.template.dto.endring.EndringRapportertInntektDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;

@Dependent
public class EndringInnholdBygger implements VedtaksbrevInnholdBygger {

    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private RapportertInntektMapper rapportertInntektMapper;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    //TODO hente fra et annet sted?
    public static final BigDecimal REDUKSJONS_FAKTOR = BigDecimal.valueOf(0.66);
    private static final int REDUSJON_PROSENT = REDUKSJONS_FAKTOR.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();


    @Inject
    public EndringInnholdBygger(
        TilkjentYtelseRepository tilkjentYtelseRepository,
        RapportertInntektMapper rapportertInntektMapper,
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    public EndringInnholdBygger() {
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var tilkjentYtelseTidslinje = tilkjentYtelseRepository.hentTidslinje(behandling.getId()).compress();

        var relevantTilkjentYtelse = resultatTidslinje.combine(tilkjentYtelseTidslinje, StandardCombinators::rightOnly,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);

        if (relevantTilkjentYtelse.isEmpty()) {
            throw new IllegalStateException("Fant ingen tilkjent ytelse i perioden" + resultatTidslinje.getLocalDateIntervals());
        }


        var rapporteInntekterTidslinje = rapportertInntektMapper.map(behandling.getId());
        var satsTidslinje = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Mangler grunnlag"))
            .getSatsTidslinje();

        var satsOgInntektTidslinje = rapporteInntekterTidslinje.combine(satsTidslinje, (p, lhs, rhs) -> {
            var rapportertInntektSum = lhs.getValue().getRapporterteInntekter().stream()
                .map(RapportertInntekt::beløp).reduce(BigDecimal.ZERO, BigDecimal::add);

            Objects.requireNonNull(rhs, "Sats kan ikke være null for periode=%s med rapportert inntekt=%s".formatted(p.toString(), rapportertInntektSum.toPlainString()));
            var sats = rhs.getValue();

            return new LocalDateSegment<>(p, new OpprinnligSatsOgRapportertInntekt(sats.dagsats(), rapportertInntektSum));
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);


        var dtoTimeline = relevantTilkjentYtelse.combine(satsOgInntektTidslinje, (p, lhs, rhs) -> {
                var ty = lhs.getValue();

                Objects.requireNonNull(rhs, "Mangler sats og rapportert inntekt for periode %s for tilkjent ytelse %s"
                    .formatted(p.toString(), ty.toString()));

                var satsOgInntekt = rhs.getValue();

                return new LocalDateSegment<>(p,
                    new EndringRapportertInntektDto(
                        new PeriodeDto(p.getFomDato(), p.getTomDato()),
                        satsOgInntekt.rapportertInntekt().longValue(),
                        ty.redusertBeløp().setScale(0, RoundingMode.HALF_UP).longValue(),
                        REDUSJON_PROSENT,
                        ty.reduksjon().setScale(0, RoundingMode.HALF_UP).longValue(),
                        satsOgInntekt.opprinnligSats().setScale(0, RoundingMode.HALF_UP).longValue(),
                        ty.dagsats().setScale(0, RoundingMode.HALF_UP).longValue()
                    )
                );
            }
            , LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_INNTEKT, new EndringDto(
            dtoTimeline.stream().findFirst().orElseThrow().getValue()
        ));
    }

    /**
     * Brukt for å kombinere flere tidslinjer
     */
    private record OpprinnligSatsOgRapportertInntekt(BigDecimal opprinnligSats, BigDecimal rapportertInntekt) {
    }
}
