package no.nav.ung.sak.domene.iay.modell;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.ung.sak.typer.Arbeidsgiver;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Filter for å hente inntekter og inntektsposter fra grunnlag. Tilbyr håndtering av skjæringstidspunkt og filtereing på inntektskilder slik
 * at en ikke trenger å implementere selv navigering av modellen.
 */
public class InntektFilter {
    public static final InntektFilter EMPTY = new InntektFilter(Collections.emptyList());

    private final Collection<Inntekt> inntekter;
    private final LocalDateTimeline<?> perioder;

    private BiPredicate<Inntekt, Inntektspost> inntektspostFilter;

    public InntektFilter(Collection<Inntekt> inntekter) {
        this(inntekter, null);
    }

    public InntektFilter(Optional<AktørInntekt> aktørInntekt) {
        this(aktørInntekt.isPresent() ? aktørInntekt.get().getInntekt() : Collections.emptyList());
    }

    public InntektFilter(Collection<Inntekt> inntekter, LocalDateTimeline<?> perioder) {
        this.inntekter = inntekter == null ? Collections.emptyList() : inntekter;
        this.perioder = perioder;
    }

    public boolean isEmpty() {
        return inntekter.isEmpty();
    }

    public InntektFilter filter(Arbeidsgiver arbeidsgiver) {
        var innt = inntekter.stream().filter(i -> Objects.equals(arbeidsgiver, i.getArbeidsgiver())).collect(Collectors.toList());
        return copyWith(innt, null);
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

    public InntektFilter i(LocalDateTimeline<?> tidslinje) {
        return copyWith(this.inntekter, tidslinje);
    }

    public List<Inntekt> getAlleInntekter() {
        return inntekter.stream()
            .toList();
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
        Collection<Inntektspost> inntektsposter = getAlleInntekter().stream().filter(i -> kilde == null || kilde.equals(i.getInntektsKilde()))
            .flatMap(i -> i.getAlleInntektsposter().stream().filter(ip -> filtrerInntektspost(i, ip)))
            .collect(Collectors.toList());
        return Collections.unmodifiableCollection(inntektsposter);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<inntekter(" + inntekter.size() + ")"
            + (perioder == null ? "" : ", perioder=" + perioder)
            + ">";
    }

    private boolean filtrerInntektspost(Inntekt inntekt, Inntektspost ip) {
        return (inntektspostFilter == null || inntektspostFilter.test(inntekt, ip))
            && skalMedEtterPeriodeVurdering(ip);
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

    private boolean skalMedEtterPeriodeVurdering(Inntektspost inntektspost) {
        if (inntektspost == null) {
            return false;
        }
        if (perioder != null) {
            return !perioder.intersection(inntektspost.getPeriode().toLocalDateInterval()).isEmpty();
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
        return copyWith(getAlleInntekter().stream().filter(filterFunc).collect(Collectors.toList()), null);
    }

    public InntektFilter filter(BiPredicate<Inntekt, Inntektspost> filterFunc) {
        var copy = copyWith(getAlleInntekter().stream()
            .filter(i -> i.getAlleInntektsposter().stream().anyMatch(ip -> filterFunc.test(i, ip)))
            .collect(Collectors.toList()), null);

        if (copy.inntektspostFilter == null)
            copy.inntektspostFilter = filterFunc;
        else
            copy.inntektspostFilter = (inntekt, inntektspost) -> filterFunc.test(inntekt, inntektspost) && this.inntektspostFilter.test(inntekt, inntektspost);
        return copy;
    }

    private InntektFilter copyWith(Collection<Inntekt> inntekter, LocalDateTimeline<?> perioder) {
        var copy = new InntektFilter(inntekter, perioder);
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
