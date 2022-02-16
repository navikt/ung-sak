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

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, LocalDateTime innsendingstidspunkt, KravDokumentType kravDokumentType, Utfall søknadsfristUtfall) {
        this.periode = periode;
        this.innsendingstidspunkt = innsendingstidspunkt;
        this.kravDokumentType = kravDokumentType;
        this.søknadsfristUtfall = søknadsfristUtfall;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(innsendingstidspunkt, that.innsendingstidspunkt)
            && Objects.equals(kravDokumentType, that.kravDokumentType)
            && Objects.equals(søknadsfristUtfall, that.søknadsfristUtfall);
    }

    public boolean equalsIgnorerPeriode(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return Objects.equals(innsendingstidspunkt, that.innsendingstidspunkt)
            && Objects.equals(kravDokumentType, that.kravDokumentType)
            && Objects.equals(søknadsfristUtfall, that.søknadsfristUtfall)
            && Objects.equals(periode.getJournalpostId(), that.periode.getJournalpostId())
            && Objects.equals(periode.getPayload(), that.periode.getPayload());
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, innsendingstidspunkt, søknadsfristUtfall);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "periode=" + periode +
            ", kravDokumentType=" + kravDokumentType +
            ", innsendingstidspunkt=" + innsendingstidspunkt +
            ", søknadsfristUtfall=" + søknadsfristUtfall +
            '>';
    }
}
