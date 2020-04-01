package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulus.beregning.v1.OmsorgspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.*;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OmsorgspengerYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<OmsorgspengerGrunnlag> {

    private static final Comparator<UttaksperiodeOmsorgspenger> COMP_PERIODE = Comparator.comparing(per -> per.getPeriode(),
        Comparator.nullsFirst(Comparator.naturalOrder()));

    private ÅrskvantumTjeneste årskvantumTjeneste;

    protected OmsorgspengerYtelsesspesifiktGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public OmsorgspengerYtelsesspesifiktGrunnlagMapper(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public OmsorgspengerGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var årskvantum = årskvantumTjeneste.hentÅrskvantumUttak(ref);
        if (årskvantum.getUttaksperioder() == null || årskvantum.getUttaksperioder().isEmpty()) {
            return new OmsorgspengerGrunnlag(Collections.emptyList());
        }

        var arbeidsforholdPerioder = årskvantum.getUttaksperioder().stream().collect(Collectors.groupingBy(
            UttaksperiodeOmsorgspenger::getUttakArbeidsforhold));
        var utbetalingsgradPrAktivitet = arbeidsforholdPerioder.entrySet().stream().map(e -> mapTilUtbetalingsgrad(e.getKey(), e.getValue())).collect(Collectors.toList());
        return new OmsorgspengerGrunnlag(utbetalingsgradPrAktivitet);
    }

    private UtbetalingsgradPrAktivitetDto mapTilUtbetalingsgrad(UttakArbeidsforhold uttakArbeidsforhold, List<UttaksperiodeOmsorgspenger> perioder) {
        var arbeidsforhold = mapTilKalkulusArbeidsforhold(uttakArbeidsforhold);
        var utbetalingsgrad = perioder.stream()
            .filter(p -> p.getUtfall() == OmsorgspengerUtfall.INNVILGET)
            .sorted(COMP_PERIODE) // stabil rekkefølge output
            .map(p -> new PeriodeMedUtbetalingsgradDto(tilKalkulusPeriode(p.getPeriode()), p.getUtbetalingsgrad().getUtbetalingsgrad()))
            .collect(Collectors.toList());
        return new UtbetalingsgradPrAktivitetDto(arbeidsforhold, utbetalingsgrad);
    }

    private UtbetalingsgradArbeidsforholdDto mapTilKalkulusArbeidsforhold(UttakArbeidsforhold arb) {
        var aktør = mapTilKalkulusAktør(arb);
        var type = mapType(arb.getType());
        var internArbeidsforholdId = mapArbeidsforholdId(arb.getArbeidsforholdId());
        var utbetalingsgradARbeidsforhold = new UtbetalingsgradArbeidsforholdDto(aktør, internArbeidsforholdId, type);
        return utbetalingsgradARbeidsforhold;
    }

    private static InternArbeidsforholdRefDto mapArbeidsforholdId(String arbeidsforholdId) {
        return arbeidsforholdId == null ? null : new InternArbeidsforholdRefDto(arbeidsforholdId);
    }

    private static Aktør mapTilKalkulusAktør(UttakArbeidsforhold arb) {
        if (arb != null && arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId().getId());
        } else if (arb != null && arb.getOrganisasjonsnummer() == null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

    private static UttakArbeidType mapType(no.nav.k9.kodeverk.uttak.UttakArbeidType type) {
        return new UttakArbeidType(type.getKode());
    }

    private static Periode tilKalkulusPeriode(no.nav.k9.sak.kontrakt.uttak.Periode periode) {
        return new Periode(periode.getFom(), periode.getTom());
    }

}
