package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Filter for å hente ytelser fra grunnlag. Tilbyr håndtering av skjæringstidspunkt og filtereing på ytelser slik
 * at en ikke trenger å implementere selv navigering av modellen.
 */
public class YtelseFilter {
    public static final YtelseFilter EMPTY = new YtelseFilter(Collections.emptyList());

    private final Collection<Ytelse> ytelser;
    private final LocalDate skjæringstidspunkt;
    private final Boolean venstreSideASkjæringstidspunkt;
    private final DatoIntervallEntitet periode;

    private Predicate<Ytelse> ytelseFilter;

    public YtelseFilter(AktørYtelse aktørYtelse) {
        this(aktørYtelse.getAlleYtelser(), null, null);
    }

    public YtelseFilter(Collection<Ytelse> inntekter) {
        this(inntekter, null, null);
    }

    public YtelseFilter(Collection<Ytelse> inntekter, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        this(inntekter, null, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
    }

    public YtelseFilter(Optional<AktørYtelse> aktørYtelse) {
        this(aktørYtelse.isPresent() ? aktørYtelse.get().getAlleYtelser() : Collections.emptyList());
    }

    public YtelseFilter(Collection<Ytelse> ytelser, DatoIntervallEntitet periode, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        this.ytelser = ytelser == null ? Collections.emptyList() : ytelser;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.venstreSideASkjæringstidspunkt = venstreSideASkjæringstidspunkt;
        this.periode = periode;
    }

    public YtelseFilter etter(LocalDate skjæringstidspunkt) {
        return copyWith(this.ytelser, null, skjæringstidspunkt, false);
    }

    public boolean isEmpty() {
        return ytelser.isEmpty();
    }

    public boolean isEmptyFiltered() {
        return getFiltrertYtelser().isEmpty();
    }

    public YtelseFilter i(DatoIntervallEntitet periode) {
        return copyWith(this.ytelser, periode, skjæringstidspunkt, true);
    }
    public YtelseFilter før(LocalDate skjæringstidspunkt) {
        return copyWith(this.ytelser, null, skjæringstidspunkt, true);
    }

    public List<Ytelse> getAlleYtelser() {
        return List.copyOf(ytelser);
    }

    /**
     * Get ytelser - filtrert for skjæringstidspunkt hvis satt på filter.
     */
    public Collection<Ytelse> getFiltrertYtelser() {
        return getFiltrertYtelser(getAlleYtelser());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "<ytelser(" + ytelser.size() + ")"
            + (skjæringstidspunkt == null ? "" : ", skjæringstidspunkt=" + skjæringstidspunkt)
            + (venstreSideASkjæringstidspunkt == null ? "" : ", venstreSideASkjæringstidspunkt=" + venstreSideASkjæringstidspunkt)
            + ">";
    }

    /**
     * Get ytelser. Filtrer for skjæringstidspunkt, etc hvis definert
     */
    private Collection<Ytelse> getFiltrertYtelser(Collection<Ytelse> ytelser) {
        Collection<Ytelse> resultat = ytelser.stream()
            .filter(yt -> (this.ytelseFilter == null || this.ytelseFilter.test(yt)) && skalMedEtterSkjæringstidspunktVurdering(yt))
            .toList();
        return Collections.unmodifiableCollection(resultat);
    }

    private boolean skalMedEtterSkjæringstidspunktVurdering(Ytelse ytelse) {
        if (periode != null) {
            return ytelse.getPeriode().overlapper(periode);
        }

        if (skjæringstidspunkt != null) {
            DatoIntervallEntitet ytelsePeriode = ytelse.getPeriode();
            if (venstreSideASkjæringstidspunkt) {
                return ytelsePeriode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1));
            } else {
                return ytelsePeriode.getFomDato().isAfter(skjæringstidspunkt) ||
                    ytelsePeriode.getFomDato().isBefore(skjæringstidspunkt.plusDays(1)) && ytelsePeriode.getTomDato().isAfter(skjæringstidspunkt);
            }
        }
        return true;
    }

    /**
     * Appliserer angitt funksjon til hver ytelse som matcher dette filteret.
     */
    public void forFilter(Consumer<Ytelse> consumer) {
        getAlleYtelser().forEach(consumer);
    }

    public YtelseFilter filter(Predicate<Ytelse> filterFunc) {
        var copy = copyWith(getFiltrertYtelser().stream().filter(filterFunc).collect(Collectors.toList()), periode, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
        if (copy.ytelseFilter == null) {
            copy.ytelseFilter = filterFunc;
        } else {
            copy.ytelseFilter = (ytelse) -> filterFunc.test(ytelse) && this.ytelseFilter.test(ytelse);
        }
        return copy;
    }

    private YtelseFilter copyWith(Collection<Ytelse> ytelser, DatoIntervallEntitet periode, LocalDate skjæringstidspunkt, Boolean venstreSideASkjæringstidspunkt) {
        var copy = new YtelseFilter(ytelser, periode, skjæringstidspunkt, venstreSideASkjæringstidspunkt);
        copy.ytelseFilter = this.ytelseFilter;
        return copy;
    }

    public boolean anyMatchFilter(Predicate<Ytelse> matcher) {
        return getAlleYtelser().stream().anyMatch(matcher);
    }

    public <R> Collection<R> mapYtelse(Function<Ytelse, R> mapper) {
        List<R> result = new ArrayList<>();
        forFilter(ytelse -> mapper.apply(ytelse));
        return Collections.unmodifiableList(result);
    }

}
