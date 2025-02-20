package no.nav.ung.sak.formidling.innhold;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.EndringDto;
import no.nav.ung.sak.formidling.template.dto.endring.EndringRapportertInntektDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

@Dependent
public class EndringInnholdBygger implements VedtaksbrevInnholdBygger  {

    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    //TODO hente fra et annet sted?
    public static final BigDecimal REDUKSJONS_FAKTOR = BigDecimal.valueOf(0.66);
    private static final int REDUSJON_PROSENT = REDUKSJONS_FAKTOR.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();


    @Inject
    public EndringInnholdBygger(
        TilkjentYtelseRepository tilkjentYtelseRepository,
        InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
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

        Set<EndringRapportertInntektDto> endringRapportertInntektDtos = relevantTilkjentYtelse.stream().map(it -> {
            var ty = it.getValue();
            return new EndringRapportertInntektDto(
                new PeriodeDto(it.getFom(), it.getTom()),
                10000, //TODO hent
                ty.redusertBeløp().setScale(0, RoundingMode.HALF_UP).longValue(),
                REDUSJON_PROSENT,
                ty.reduksjon().setScale(0, RoundingMode.HALF_UP).longValue(),
                0, //TODO hent
                ty.dagsats().setScale(0, RoundingMode.HALF_UP).longValue()
                );

        }).collect(Collectors.toSet());

        return new TemplateInnholdResultat(DokumentMalType.ENDRING_DOK, TemplateType.ENDRING_INNTEKT, new EndringDto(
            endringRapportertInntektDtos.stream().findFirst().orElseThrow()
        ));
    }
}
