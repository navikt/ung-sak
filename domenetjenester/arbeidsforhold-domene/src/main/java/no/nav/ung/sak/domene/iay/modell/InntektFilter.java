package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Arbeidsgiver;

/**
 * Filter for å hente inntekter og inntektsposter fra grunnlag. Tilbyr håndtering av skjæringstidspunkt og filtereing på inntektskilder slik
 * at en ikke trenger å implementere selv navigering av modellen.
 */
public class InntektFilter {
    public static final InntektFilter EMPTY = new InntektFilter(Collections.emptyList());

    private final Collection<Inntekt> inntekter;
    private final LocalDate skjæringstidspunkt;
    private final Boolean venstreSideASkjæringstidspunkt;
    private final DatoIntervallEntitet periode;

    private BiPredicate<Inntekt, Inntektspost> inntektspostFilter;

    public InntektFilter(AktørInntekt aktørInntekt) {
        this(aktørInntekt.getInntekt(), null, null);
    }

    public InntektFilter(Collection<Inntekt> inntekter) {
        this(inntekter, null, null);
    }

    public InntektFilter(Collection<Inntekt> inntekter, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        this(inntekter, null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter(Optional<AktørInntekt> aktørInntekt) {
        this(aktørInntekt.isPresent() ? aktørInntekt.get().getInntekt() : Collections.emptyList());
    }

    public InntektFilter(Collection<Inntekt> inntekter, DatoIntervallEntitet periode, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        this.inntekter = inntekter == null ? Collections.emptyList() : inntekter;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.venstreSideASkjæringstidspunkt = venstreSideASkjæringstidspunkt;
        this.periode = periode;
    }

    public InntektFilter etter(LocalDate skjæringstidspunkt) {
        return copyWith(this.inntekter, null, skjæringstidspunkt, false);
    }

    public boolean isEmpty() {
        return inntekter.isEmpty();
    }

    public InntektFilter filter(Arbeidsgiver arbeidsgiver) {
        var innt = inntekter.stream().filter(i -> Objects.equals(arbeidsgiver, i.getArbeidsgiver())).collect(Collectors.toList());
        return copyWith(innt, null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filter(InntektsKilde kilde) {
        return copyWith(getAlleInntekter(kilde), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filter(InntektspostType inntektspostType) {
        return filter(Set.of(inntektspostType));
    }

    public InntektFilter filter(InntektspostType... inntektspostTyper) {
        return filter(Set.of(inntektspostTyper));
    }

    public InntektFilter filter(Set<InntektspostType> typer) {
        return filter((inntekt, inntektspost) -> typer.contains(inntektspost.getInntektspostType()));
    }

    public InntektFilter filterBeregnetSkatt() {
        return copyWith(getAlleInntektBeregnetSkatt(), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filterBeregningsgrunnlag() {
        return copyWith(getAlleInntektBeregningsgrunnlag(), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filterPensjonsgivende() {
        return copyWith(getAlleInntektPensjonsgivende(), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filterSammenligningsgrunnlag() {
        return copyWith(getAlleInntektSammenligningsgrunnlag(), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public Collection<Inntektspost> filtrer(Inntekt inntekt, Collection<Inntektspost> inntektsposter) {
        if (inntektsposter == null)
            return Collections.emptySet();
        return inntektsposter.stream()
            .filter(ip -> filtrerInntektspost(inntekt, ip))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public InntektFilter før(LocalDate skjæringstidspunkt) {
        return copyWith(this.inntekter, null, skjæringstidspunkt, true);
    }

    public InntektFilter i(DatoIntervallEntitet periode) {
        return copyWith(this.inntekter, periode, skjæringstidspunkt, true);
    }

    public List<Inntekt> getAlleInntektBeregnetSkatt() {
        return getAlleInntekter(InntektsKilde.SIGRUN);
    }

    public List<Inntekt> getAlleInntektBeregningsgrunnlag() {
        return getAlleInntekter(InntektsKilde.INNTEKT_BEREGNING);
    }

    public List<Inntekt> getAlleInntekter(InntektsKilde kilde) {
        return inntekter.stream()
            .filter(it -> kilde == null || kilde.equals(it.getInntektsKilde()))
            .toList();
    }

    public List<Inntekt> getAlleInntekter() {
        return getAlleInntekter(null);
    }

    public List<Inntekt> getAlleInntektPensjonsgivende() {
        return getAlleInntekter(InntektsKilde.INNTEKT_OPPTJENING);
    }

    public List<Inntekt> getAlleInntektSammenligningsgrunnlag() {
        return getAlleInntekter(InntektsKilde.INNTEKT_SAMMENLIGNING);
    }

    /**
     * Get alle inntektsposter - fullstendig ufiltrert og uten hensyn til konfigurert skjæringstidspunkt.
     */
    public Collection<Inntektspost> getAlleInntektsposter() {
        return Collections.unmodifiableCollection(inntekter.stream().flatMap(i -> i.getAlleInntektsposter().stream())
            .collect(Collectors.toList()));
    }

    /**
     * Get inntektsposter - filtrert for skjæringstidspunkt hvis satt på filter.
     */
    public Collection<Inntektspost> getFiltrertInntektsposter() {
        return getInntektsposter((InntektsKilde) null);
    }

    /**
     * Get inntektsposter - filtrert for skjæringstidspunkt, inntektsposttype, etc hvis satt på filter.
     */
    public Collection<Inntektspost> getInntektsposter(InntektsKilde kilde) {
        Collection<Inntektspost> inntektsposter = getAlleInntekter(null).stream().filter(i -> kilde == null || kilde.equals(i.getInntektsKilde()))
            .flatMap(i -> i.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(i, ip)))
            .collect(Collectors.toList());
        return Collections.unmodifiableCollection(inntektsposter);
    }

    public Collection<Inntektspost> getInntektsposterBeregningsgrunnlag() {
        return getInntektsposter(getAlleInntektBeregningsgrunnlag());
    }

    public Collection<Inntektspost> getInntektsposterPensjonsgivende() {
        return getInntektsposter(getAlleInntektPensjonsgivende());
    }

    public Collection<Inntektspost> getInntektsposterSammenligningsgrunnlag() {
        return getInntektsposter(getAlleInntektSammenligningsgrunnlag());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<inntekter(" + inntekter.size() + ")"
            + (skjæringstidspunkt == null ? "" : ", skjæringstidspunkt=" + skjæringstidspunkt)
            + (venstreSideASkjæringstidspunkt == null ? "" : ", venstreSideASkjæringstidspunkt=" + venstreSideASkjæringstidspunkt)
            + ">";
    }

    private boolean filtrerInntektspost(Inntekt inntekt, Inntektspost ip) {
        return (inntektspostFilter == null || inntektspostFilter.test(inntekt, ip))
            && skalMedEtterSkjæringstidspunktVurdering(ip);
    }

    /**
     * Get inntektsposter. Filtrer for skjæringstidspunkt, inntektsposttype etc hvis definert
     */
    private Collection<Inntektspost> getInntektsposter(Collection<Inntekt> inntekter) {
        Collection<Inntektspost> inntektsposter = inntekter.stream()
            .flatMap(i -> i.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(i, ip)))
            .collect(Collectors.toList());
        return Collections.unmodifiableCollection(inntektsposter);
    }

    private Collection<Inntektspost> getFiltrertInntektsposter(Inntekt inntekt) {
        Collection<Inntektspost> inntektsposter = inntekt.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(inntekt, ip))
            .collect(Collectors.toList());
        return Collections.unmodifiableCollection(inntektsposter);
    }

    private boolean skalMedEtterSkjæringstidspunktVurdering(Inntektspost inntektspost) {
        if (inntektspost == null) {
            return false;
        }
        if (periode != null) {
            return inntektspost.getPeriode().overlapper(periode);
        }
        if (skjæringstidspunkt != null) {
            DatoIntervallEntitet inntektspostPeriode = inntektspost.getPeriode();
            if (venstreSideASkjæringstidspunkt) {
                return inntektspostPeriode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1));
            } else {
                return inntektspostPeriode.getFomDato().isAfter(skjæringstidspunkt) ||
                    inntektspostPeriode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1)) && inntektspostPeriode.getTomDato().isAfter(skjæringstidspunkt);
            }
        }
        return true;
    }

    /**
     * Appliserer angitt funksjon til hver inntekt og for inntekts inntektsposter som matcher dette filteret.
     */
    public void forFilter(BiConsumer<Inntekt, Collection<Inntektspost>> consumer) {
        getAlleInntekter().forEach(i -> {
            var inntektsposterFiltrert = getFiltrertInntektsposter(i).stream().filter(ip -> filtrerInntektspost(i, ip)).collect(Collectors.toList());
            consumer.accept(i, inntektsposterFiltrert);
        });
    }

    public InntektFilter filter(Predicate<Inntekt> filterFunc) {
        return copyWith(getAlleInntekter().stream().filter(filterFunc).collect(Collectors.toList()), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public InntektFilter filter(BiPredicate<Inntekt, Inntektspost> filterFunc) {
        var copy = copyWith(getAlleInntekter().stream()
            .filter(i -> i.getAlleInntektsposter().stream().anyMatch(ip -> filterFunc.test(i, ip)))
            .collect(Collectors.toList()), null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);

        if (copy.inntektspostFilter == null)
            copy.inntektspostFilter = filterFunc;
        else
            copy.inntektspostFilter = (inntekt, inntektspost) -> filterFunc.test(inntekt, inntektspost) && this.inntektspostFilter.test(inntekt, inntektspost);
        return copy;
    }

    private InntektFilter copyWith(Collection<Inntekt> inntekter, DatoIntervallEntitet periode, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        var copy = new InntektFilter(inntekter, periode, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
        copy.inntektspostFilter = this.inntektspostFilter;
        return copy;
    }

    public boolean anyMatchFilter(BiPredicate<Inntekt, Inntektspost> matcher) {
        return getAlleInntekter().stream().anyMatch(i -> getFiltrertInntektsposter(i).stream().anyMatch(ip -> matcher.test(i, ip)));
    }

    public <R> Collection<R> mapInntektspost(BiFunction<Inntekt, Inntektspost, R> mapper) {
        List<R> result = new ArrayList<>();
        forFilter((inntekt, inntektsposter) -> inntektsposter.forEach(ip -> result.add(mapper.apply(inntekt, ip))));
        return Collections.unmodifiableList(result);
    }

}
