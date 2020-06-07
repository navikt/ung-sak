package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.Objects;

import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private Aktivitet aktivitet;
    private Boolean iPermisjon;
    private Boolean ikkeIArbeid;
    private Boolean avslåttInngangsvilkår;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, Boolean iPermisjon, Boolean ikkeIArbeid, Boolean avslåttInngangsvilkår) {
        this.periode = periode;
        this.iPermisjon = iPermisjon;
        this.ikkeIArbeid = ikkeIArbeid;
        if (periode != null && periode.getAktivitetType() != null) {
            this.aktivitet = new Aktivitet(periode.getArbeidsgiver(), periode.getArbeidsforholdRef() != null ? periode.getArbeidsforholdRef() : InternArbeidsforholdRef.nullRef());
        } else {
            this.aktivitet = null;
        }
        this.avslåttInngangsvilkår = avslåttInngangsvilkår;
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

    public Boolean getErIkkeIArbeid() {
        return ikkeIArbeid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return Objects.equals(avslåttInngangsvilkår, that.avslåttInngangsvilkår)
            && Objects.equals(ikkeIArbeid, that.ikkeIArbeid)
            && Objects.equals(iPermisjon, that.iPermisjon)
            && Objects.equals(aktivitet, that.aktivitet)
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
        return Objects.hash(periode.getFraværPerDag(), periode.getAktivitetType(), aktivitet, avslåttInngangsvilkår, iPermisjon, ikkeIArbeid);
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            ", avslåttInngangsvilkår=" + avslåttInngangsvilkår +
            ", iPermisjon=" + iPermisjon +
            ", ikkeIArbeid=" + ikkeIArbeid +
            '}';
    }

}
