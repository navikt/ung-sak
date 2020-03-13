package no.nav.k9.sak.domene.uttak.input;

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
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakArbeidsforhold;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class UttakAktivitetStatusPeriode implements Comparable<UttakAktivitetStatusPeriode> {

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

    private static final Comparator<UttakAktivitetStatusPeriode> COMP = Comparator
        .comparing(UttakAktivitetStatusPeriode::getUttakArbeidsforhold, Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getFom(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getTom(), Comparator.nullsLast(Comparator.naturalOrder()));

    private final LocalDate fom;

    private final LocalDate tom;

    private final UttakArbeidsforhold arbeidsforhold;

    public UttakAktivitetStatusPeriode(AktivitetStatus aktivitetStatus,
                                       LocalDate fom,
                                       LocalDate tom,
                                       Arbeidsgiver arbeidsgiver,
                                       InternArbeidsforholdRef arbeidsforholdRef) {
        this.fom = fom;
        this.tom = tom;

        var uttakArbeidType = UTTAK_ARBEID_TYPER.stream()
            .filter(p -> p.getValue().test(aktivitetStatus)).findFirst().map(UttakArbeidTypeMapping::getKey)
            .orElse(UttakArbeidType.ANNET);

        this.arbeidsforhold = new UttakArbeidsforhold(arbeidsgiver.getOrgnr(), arbeidsgiver.getAktørId(), uttakArbeidType, arbeidsforholdRef.getReferanse());
    }

    /** Andel uten arbeidsgiver. (eks frilanser, selvstendig næringsdrivende). */
    public UttakAktivitetStatusPeriode(AktivitetStatus aktivitetStatus, LocalDate fom, LocalDate tom) {
        this(aktivitetStatus, fom, tom, null, null);
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public UttakArbeidsforhold getUttakArbeidsforhold() {
        return arbeidsforhold;
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforhold).map(ua -> InternArbeidsforholdRef.ref(ua.getArbeidsforholdId()));
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsforhold).flatMap(ua -> toArbeidsgiver(ua));
    }

    private Optional<Arbeidsgiver> toArbeidsgiver(UttakArbeidsforhold ua) {
        if (ua.getAktørId() != null) {
            return Optional.of(Arbeidsgiver.fra(ua.getAktørId()));
        } else if (ua.getOrganisasjonsnummer() != null) {
            return Optional.of(Arbeidsgiver.virksomhet(ua.getOrganisasjonsnummer()));
        } else {
            return Optional.empty();
        }
    }

    public UttakArbeidType getUttakArbeidType() {
        return arbeidsforhold.getType();
    }

    public AktivitetStatus getAktivitetStatus() {
        return getUttakArbeidType().getAktivitetStatus();
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "fom=" + fom
            + ", tom=" + tom
            + ", arbeidsforhold=" + arbeidsforhold
            + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !obj.getClass().equals(this.getClass())) {
            return false;
        }
        UttakAktivitetStatusPeriode other = (UttakAktivitetStatusPeriode) obj;
        return Objects.equals(this.arbeidsforhold, other.arbeidsforhold)
            && Objects.equals(this.fom, other.fom)
            && Objects.equals(this.tom, other.tom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsforhold, fom, tom);
    }

    @Override
    public int compareTo(UttakAktivitetStatusPeriode o) {
        return COMP.compare(this, o);
    }
}
