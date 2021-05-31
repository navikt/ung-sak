package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.Objects;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;

public class InntektsmeldingMedPerioder {

    private JournalpostId journalpostId;
    private DatoIntervallEntitet periode;

    public InntektsmeldingMedPerioder(JournalpostId journalpostId, DatoIntervallEntitet periode) {
        this.journalpostId = Objects.requireNonNull(journalpostId);
        this.periode = periode;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InntektsmeldingMedPerioder that = (InntektsmeldingMedPerioder) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return "InnteksmeldingMedPerioder{" +
            "journalpostId=" + journalpostId +
            ", periode=" + periode +
            '}';
    }
}
