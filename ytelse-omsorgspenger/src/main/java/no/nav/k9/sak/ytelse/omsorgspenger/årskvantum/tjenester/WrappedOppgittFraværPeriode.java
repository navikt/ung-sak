package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private Aktivitet aktivitet;
    private Map<Vilkår, Utfall> vurderteVilkår = new HashMap<>();

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, Map<Vilkår, Utfall> vurderteVilkår) {
        this.periode = periode;
        if (periode != null && periode.getAktivitetType() != null) {
            this.aktivitet = new Aktivitet(periode.getAktivitetType(), periode.getArbeidsgiver(), periode.getArbeidsforholdRef());
        } else {
            this.aktivitet = null;
        }
        this.vurderteVilkår = vurderteVilkår;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public Map<Vilkår, Utfall> getVurderteVilkår() {
        return vurderteVilkår;
    }

    public void setVurderteVilkår(Map<Vilkår, Utfall> vurderteVilkår) {
        this.vurderteVilkår = vurderteVilkår;
    }

    public Aktivitet getAktivitet() {
        return aktivitet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return vurderteVilkår == that.vurderteVilkår
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

    public boolean erAvslått() {
        for(no.nav.k9.aarskvantum.kontrakter.Vilkår vilkår : vurderteVilkår.keySet()) {
            if (vurderteVilkår.getOrDefault(vilkår, no.nav.k9.aarskvantum.kontrakter.Utfall.INNVILGET).equals(no.nav.k9.aarskvantum.kontrakter.Utfall.AVSLÅTT)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.getFraværPerDag(), periode.getAktivitetType(), aktivitet, vurderteVilkår);
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            ", avslått=" + vurderteVilkår +
            '}';
    }

}
