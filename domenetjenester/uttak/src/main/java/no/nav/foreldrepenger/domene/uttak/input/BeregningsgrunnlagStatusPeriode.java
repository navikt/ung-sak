package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class BeregningsgrunnlagStatusPeriode implements Comparable<BeregningsgrunnlagStatusPeriode> {

    private static class UttakArbeidTypeMapping extends SimpleEntry<UttakArbeidType, Predicate<AktivitetStatus>> {

        public UttakArbeidTypeMapping(UttakArbeidType key, Predicate<AktivitetStatus> value) {
            super(key, value);
        }
    }
    private static final List<UttakArbeidTypeMapping> UTTAK_ARBEID_TYPER = List.of(
        new UttakArbeidTypeMapping(UttakArbeidType.FRILANSER, (ast) -> ast.erFrilanser()),
        new UttakArbeidTypeMapping(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, (ast) -> ast.erSelvstendigNæringsdrivende()),
        new UttakArbeidTypeMapping(UttakArbeidType.ARBEIDSTAKER, (ast) -> ast.erArbeidstaker()),
        new UttakArbeidTypeMapping(UttakArbeidType.ANNET, (ast) -> true));

    private static final Comparator<BeregningsgrunnlagStatusPeriode> COMP = Comparator
        .comparing((BeregningsgrunnlagStatusPeriode dto) -> dto.arbeidsgiver == null ? null : dto.arbeidsgiver.getIdentifikator(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing((BeregningsgrunnlagStatusPeriode dto) -> dto.arbeidsforholdRef == null ? null : dto.arbeidsforholdRef.getReferanse(),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private final LocalDate fom;

    private final LocalDate tom;
    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final Arbeidsgiver arbeidsgiver;
    private UttakArbeidType uttakArbeidType;
    private AktivitetStatus aktivitetStatus;

    public BeregningsgrunnlagStatusPeriode(AktivitetStatus aktivitetStatus,
                                           LocalDate fom,
                                           LocalDate tom,
                                           Arbeidsgiver arbeidsgiver,
                                           InternArbeidsforholdRef arbeidsforholdRef) {
        this.fom = fom;
        this.tom = tom;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.arbeidsgiver = arbeidsgiver;
        this.aktivitetStatus = aktivitetStatus;
        this.uttakArbeidType = UTTAK_ARBEID_TYPER.stream().filter(p -> p.getValue().test(aktivitetStatus)).findFirst().map(UttakArbeidTypeMapping::getKey)
            .orElse(null);
    }

    /** Andel uten arbeidsgiver. (eks frilanser, selvstendig næringsdrivende). */
    public BeregningsgrunnlagStatusPeriode(AktivitetStatus aktivitetStatus, LocalDate fom, LocalDate tom) {
        this(aktivitetStatus, fom, tom, null, null);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforholdRef);
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "fom=" + fom
            + ", tom=" + tom
            + ", aktivitetStatus=" + aktivitetStatus
            + (arbeidsgiver == null ? "" : ", arbeidsgiver=" + arbeidsgiver)
            + (arbeidsforholdRef == null ? "" : ", arbeidsforholdRef=" + arbeidsforholdRef)
            + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        BeregningsgrunnlagStatusPeriode other = (BeregningsgrunnlagStatusPeriode) obj;
        return Objects.equals(this.arbeidsforholdRef, other.arbeidsforholdRef)
            && Objects.equals(this.arbeidsgiver, other.arbeidsgiver)
            && Objects.equals(this.uttakArbeidType, other.uttakArbeidType)
            && Objects.equals(this.aktivitetStatus, other.aktivitetStatus)
            && Objects.equals(this.fom, other.fom)
            && Objects.equals(this.tom, other.tom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsforholdRef, uttakArbeidType, fom, tom);
    }

    @Override
    public int compareTo(BeregningsgrunnlagStatusPeriode o) {
        return COMP.compare(this, o);
    }
}
