package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.*;
import no.nav.folketrygdloven.kalkulus.felles.v1.*;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.*;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<OmsorgspengerGrunnlag> {

    private static final Comparator<Uttaksperiode> COMP_PERIODE = comparing(uttaksperiode -> uttaksperiode.getPeriode().getFom());

    private ÅrskvantumTjeneste årskvantumTjeneste;

    protected OmsorgspengerYtelsesspesifiktGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public OmsorgspengerYtelsesspesifiktGrunnlagMapper(ÅrskvantumTjeneste årskvantumTjeneste) {
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    private static InternArbeidsforholdRefDto mapArbeidsforholdId(String arbeidsforholdId) {
        return arbeidsforholdId == null ? null : new InternArbeidsforholdRefDto(arbeidsforholdId);
    }

    private static Aktør mapTilKalkulusAktør(Arbeidsforhold arb) {
        if (arb != null && arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId());
        } else if (arb != null && arb.getOrganisasjonsnummer() != null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

    private static UttakArbeidType mapType(String type) {
        return UttakArbeidType.fraKode(type);
    }

    private static Periode tilKalkulusPeriode(LukketPeriode periode) {
        return new Periode(periode.getFom(), periode.getTom());
    }

    @Override
    public OmsorgspengerGrunnlag lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        List<Aktivitet> aktiviteter = hentAktiviteter(ref);

        if (aktiviteter.isEmpty()) {
            return new OmsorgspengerGrunnlag(Collections.emptyList(), Collections.emptyList());
        }

        // Kalkulus forventer å ikke få duplikate arbeidsforhold, så vi samler alle perioder pr arbeidsforhold/aktivitet
        var gruppertPrAktivitet = aktiviteter.stream()
            .filter(e -> !e.getUttaksperioder().isEmpty())
            .collect(Collectors.groupingBy(a -> mapTilKalkulusArbeidsforhold(a.getArbeidsforhold())));


        var utbetalingsgradPrAktivitet = gruppertPrAktivitet
            .entrySet()
            .stream()
            .map(e -> {
                var utbetalingsgraderForVilkårsperiode = filtrerForVilkårsperiode(vilkårsperiode, e.getValue().stream().flatMap(a -> a.getUttaksperioder().stream()));
                return mapTilUtbetalingsgrad(utbetalingsgraderForVilkårsperiode, e.getKey());
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return new OmsorgspengerGrunnlag(utbetalingsgradPrAktivitet, mapSøktePerioder(aktiviteter));
    }

    private static List<SøktPeriode> mapSøktePerioder(List<Aktivitet> aktiviteter) {
        var harBrukerSøktSegmenter = aktiviteter.stream()
            .flatMap(a -> a.getUttaksperioder().stream())
            .filter(p -> p.getUtbetalingsgrad().compareTo(BigDecimal.ZERO) > 0)
            .map(p -> new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), erKravFraBruker(p)))
            .toList();
        var brukerHarSøktTidslinje = new LocalDateTimeline<>(harBrukerSøktSegmenter, ellerKombinator());

        return brukerHarSøktTidslinje.toSegments()
            .stream().map(s -> new SøktPeriode(new Periode(s.getFom(), s.getTom()), s.getValue()))
            .toList();
    }

    private static boolean erKravFraBruker(Uttaksperiode p) {
        return Boolean.FALSE.equals(p.getRefusjonTilArbeidsgiver());
    }

    private static LocalDateSegmentCombinator<Boolean, Boolean, Boolean> ellerKombinator() {
        // sjekker ikkje null sidan den kun brukes i konstruktør
        return (di, lhs, rhs) -> new LocalDateSegment<>(di, lhs.getValue() || rhs.getValue());
    }

    @NotNull
    private List<Uttaksperiode> filtrerForVilkårsperiode(DatoIntervallEntitet vilkårsperiode, Stream<Uttaksperiode> uttaksperiodeStream) {
        return uttaksperiodeStream
            .filter(it -> vilkårsperiode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFom(), it.getPeriode().getTom())))
            .collect(Collectors.toList());
    }

    @NotNull
    private List<Aktivitet> hentAktiviteter(BehandlingReferanse ref) {
        var fullUttaksplan = årskvantumTjeneste.hentFullUttaksplan(ref.getSaksnummer());
        return fullUttaksplan.getAktiviteter();
    }

    private UtbetalingsgradPrAktivitetDto mapTilUtbetalingsgrad(List<Uttaksperiode> perioder, AktivitetDto arbeidsforhold) {
        var utbetalingsgrad = mapUtbetalingsgradPerioder(perioder);

        if (perioder.size() != utbetalingsgrad.size()) {
            throw new IllegalArgumentException("Utvikler-feil: Skal ikke komme til kalkulus uten innvilgede perioder for " + arbeidsforhold + ", angitte uttaksperioder: " + perioder);
        }
        if (perioder.isEmpty()) {
            return null;
        }
        return new UtbetalingsgradPrAktivitetDto(arbeidsforhold, utbetalingsgrad);
    }

    @NotNull
    private List<PeriodeMedUtbetalingsgradDto> mapUtbetalingsgradPerioder(List<Uttaksperiode> perioder) {
        return perioder.stream()
            .sorted(COMP_PERIODE) // stabil rekkefølge output
            .map(p -> new PeriodeMedUtbetalingsgradDto(tilKalkulusPeriode(p.getPeriode()), mapUtbetalingsgrad(p)))
            .collect(Collectors.toList());
    }

    @NotNull
    private BigDecimal mapUtbetalingsgrad(Uttaksperiode p) {
        if (!Utfall.INNVILGET.equals(p.getUtfall()) && BigDecimal.ZERO.compareTo(p.getUtbetalingsgrad()) != 0) {
            throw new IllegalStateException("Uttaksperiode med utfall=" + p.getUtfall()
                + " og utbetalingsgrad(" + p.getUtbetalingsgrad()
                + ") er ikke 0 som forventet");
        }
        return p.getUtbetalingsgrad();
    }

    private AktivitetDto mapTilKalkulusArbeidsforhold(Arbeidsforhold arb) {
        if (erTypeMedArbeidsforhold(arb)) {
            var aktør = mapTilKalkulusAktør(arb);
            var type = mapType(arb.getType());
            var internArbeidsforholdId = mapArbeidsforholdId(arb.getArbeidsforholdId());
            return new AktivitetDto(aktør, internArbeidsforholdId, type);
        } else {
            return new AktivitetDto(null, null, mapType(arb.getType()));
        }
    }

    private boolean erTypeMedArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.ARBEIDSTAKER.getKode());
    }

}
