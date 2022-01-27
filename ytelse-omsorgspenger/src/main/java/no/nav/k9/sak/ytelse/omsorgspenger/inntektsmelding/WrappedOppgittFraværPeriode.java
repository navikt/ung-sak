package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private LocalDateTime innsendingstidspunkt;
    private KravDokumentType kravDokumentType;
    private Utfall søknadsfristUtfall;
    private boolean konfliktImSøknad;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, LocalDateTime innsendingstidspunkt, KravDokumentType kravDokumentType, Utfall søknadsfristUtfall, boolean konfliktImSøknad) {
        this.periode = periode;
        this.innsendingstidspunkt = innsendingstidspunkt;
        this.kravDokumentType = kravDokumentType;
        this.søknadsfristUtfall = søknadsfristUtfall;
        this.konfliktImSøknad = konfliktImSøknad;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public Utfall getSøknadsfristUtfall() {
        return søknadsfristUtfall;
    }

    public KravDokumentType getKravDokumentType() {
        return kravDokumentType;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    public boolean getKonfliktImSøknad() {
        return konfliktImSøknad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return periodeEquals(that)
            && Objects.equals(innsendingstidspunkt, that.innsendingstidspunkt)
            && Objects.equals(kravDokumentType, that.kravDokumentType)
            && Objects.equals(søknadsfristUtfall, that.søknadsfristUtfall)
            && Objects.equals(konfliktImSøknad, that.konfliktImSøknad);
    }

    private boolean periodeEquals(WrappedOppgittFraværPeriode that) {
        if (this.periode != null && that.periode != null) {
            return this.periode.equals(that.periode);
        } else
            return this.periode == null && that.periode == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.hashCode(), innsendingstidspunkt, søknadsfristUtfall, konfliktImSøknad);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "periode=" + periode +
            ", kravDokumentType=" + kravDokumentType +
            ", innsendingstidspunkt=" + innsendingstidspunkt +
            ", søknadsfristUtfall=" + søknadsfristUtfall +
            ", konfliktImSøknad=" + konfliktImSøknad +
            '>';
    }
}
