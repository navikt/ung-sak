package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerNærståendeGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;

@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
@ApplicationScoped
public class PleiepengerGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<YtelsespesifiktGrunnlagDto> {

    private UttakTjeneste uttakRestKlient;

    public PleiepengerGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public PleiepengerGrunnlagMapper(UttakTjeneste uttakRestKlient) {
        this.uttakRestKlient = uttakRestKlient;
    }

    @Override
    public YtelsespesifiktGrunnlagDto lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(ref.getBehandlingUuid(), false);

        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader = new ArrayList<>();
        if (uttaksplan != null) {
            utbetalingsgrader = uttaksplan.getPerioder()
                .entrySet()
                .stream()
                .filter(it -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getKey().getFom(), it.getKey().getTom())))
                .flatMap(e -> lagUtbetalingsgrad(e.getKey(), e.getValue()).entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
                .entrySet()
                .stream()
                .map(e -> new UtbetalingsgradPrAktivitetDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        }

        return mapTilYtelseSpesifikkType(ref, utbetalingsgrader);
    }

    private YtelsespesifiktGrunnlagDto mapTilYtelseSpesifikkType(BehandlingReferanse ref, List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader) {
        return switch (ref.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN -> new PleiepengerSyktBarnGrunnlag(utbetalingsgrader);
            case PLEIEPENGER_NÆRSTÅENDE -> new PleiepengerNærståendeGrunnlag(utbetalingsgrader);
            default -> throw new IllegalStateException("Ikke støttet ytelse for kalkulus Pleiepenger: " + ref.getFagsakYtelseType());
        };
    }

    private Map<UtbetalingsgradArbeidsforholdDto, PeriodeMedUtbetalingsgradDto> lagUtbetalingsgrad(LukketPeriode periode, UttaksperiodeInfo plan) {
        var perArbeidsforhold = plan.getUtbetalingsgrader()
            .stream()
            .collect(Collectors.toMap(this::mapUtbetalingsgradArbeidsforhold, Utbetalingsgrader::getUtbetalingsgrad));

        Map<UtbetalingsgradArbeidsforholdDto, PeriodeMedUtbetalingsgradDto> res = new HashMap<>();
        for (var entry : perArbeidsforhold.entrySet()) {
            var utbetalingsgradPeriode = lagPeriode(periode, entry.getValue());
            res.put(entry.getKey(), utbetalingsgradPeriode);
        }
        return res;
    }

    private UtbetalingsgradArbeidsforholdDto mapUtbetalingsgradArbeidsforhold(Utbetalingsgrader utbGrad) {
        Arbeidsforhold arbeidsforhold = utbGrad.getArbeidsforhold();
        if (erTypeMedArbeidsforhold(arbeidsforhold)) {
            return lagArbeidsforhold(arbeidsforhold);
        } else {
            return new UtbetalingsgradArbeidsforholdDto(null, null, mapUttakArbeidType(arbeidsforhold));
        }
    }

    private boolean erTypeMedArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.ARBEIDSTAKER.getKode()) ||
            arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV.getKode());
    }

    private PeriodeMedUtbetalingsgradDto lagPeriode(LukketPeriode periode, BigDecimal utbetalingsgrad) {
        var kalkulusPeriode = new no.nav.folketrygdloven.kalkulus.felles.v1.Periode(periode.getFom(), periode.getTom());
        return new PeriodeMedUtbetalingsgradDto(kalkulusPeriode, utbetalingsgrad);
    }

    private UtbetalingsgradArbeidsforholdDto lagArbeidsforhold(Arbeidsforhold arb) {
        return new UtbetalingsgradArbeidsforholdDto(lagAktør(arb),
            arb.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(arb.getArbeidsforholdId()) : null,
            mapUttakArbeidType(arb));
    }

    private UttakArbeidType mapUttakArbeidType(Arbeidsforhold arb) {
        return new UttakArbeidType(arb.getType());
    }

    private Aktør lagAktør(Arbeidsforhold arb) {
        if (arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId());
        } else if (arb.getOrganisasjonsnummer() != null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

}
