package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.SamtidigKravStatus;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private LocalDateTime innsendingstidspunkt;
    private Aktivitet aktivitet;
    private Boolean iPermisjon;
    private ArbeidStatus arbeidStatus;
    private Boolean avslåttInngangsvilkår;
    private SamtidigKravStatus samtidigeKrav;
    private Utfall utfallNyoppstartetVilkår;

    public WrappedOppgittFraværPeriode(ArbeidStatus arbeidStatus) {
        this(null, null, null, arbeidStatus, null, null, null);
    }

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, LocalDateTime innsendingstidspunkt, Boolean iPermisjon, ArbeidStatus arbeidStatus, Boolean avslåttInngangsvilkår, SamtidigKravStatus samtidigeKrav, Utfall utfallNyoppstartetVilkår) {
        this.periode = periode;
        this.innsendingstidspunkt = innsendingstidspunkt;
        this.iPermisjon = iPermisjon;
        this.arbeidStatus = arbeidStatus;
        this.samtidigeKrav = samtidigeKrav;
        if (periode != null && periode.getAktivitetType() != null) {
            this.aktivitet = new Aktivitet(periode.getAktivitetType(), periode.getArbeidsgiver(), periode.getArbeidsforholdRef() != null ? periode.getArbeidsforholdRef() : InternArbeidsforholdRef.nullRef());
        } else {
            this.aktivitet = null;
        }
        this.avslåttInngangsvilkår = avslåttInngangsvilkår;
        this.utfallNyoppstartetVilkår = utfallNyoppstartetVilkår;
    }

    public WrappedOppgittFraværPeriode(Utfall utfallNyoppstartetVilkår) {
        this.utfallNyoppstartetVilkår = utfallNyoppstartetVilkår;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public Boolean getErAvslåttInngangsvilkår() {
        return avslåttInngangsvilkår;
    }

    public Aktivitet getAktivitet() {
        return aktivitet;
    }

    public Boolean getErIPermisjon() {
        return iPermisjon;
    }

    public ArbeidStatus getArbeidStatus() {
        return arbeidStatus;
    }

    public SamtidigKravStatus getSamtidigeKrav() {
        return samtidigeKrav;
    }

    public Utfall getUtfallNyoppstartetVilkår() {
        return utfallNyoppstartetVilkår;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return Objects.equals(avslåttInngangsvilkår, that.avslåttInngangsvilkår)
            && Objects.equals(innsendingstidspunkt, that.innsendingstidspunkt)
            && Objects.equals(arbeidStatus, that.arbeidStatus)
            && Objects.equals(iPermisjon, that.iPermisjon)
            && Objects.equals(aktivitet, that.aktivitet)
            && Objects.equals(samtidigeKrav, that.samtidigeKrav)
            && Objects.equals(utfallNyoppstartetVilkår, that.utfallNyoppstartetVilkår)
            && periodeEquals(that);
    }

    private boolean periodeEquals(WrappedOppgittFraværPeriode that) {
        if (this.periode != null && that.periode != null) {
            return Objects.equals(periode.getFraværPerDag(), that.periode.getFraværPerDag())
                && Objects.equals(periode.getAktivitetType(), that.periode.getAktivitetType());
        } else
            return this.periode == null && that.periode == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getFraværPerDag(), periode.getAktivitetType(), aktivitet, avslåttInngangsvilkår, iPermisjon, arbeidStatus, innsendingstidspunkt, samtidigeKrav, utfallNyoppstartetVilkår);
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            ", avslåttInngangsvilkår=" + avslåttInngangsvilkår +
            ", iPermisjon=" + iPermisjon +
            ", arbeidStatus=" + arbeidStatus +
            '}';
    }

}
