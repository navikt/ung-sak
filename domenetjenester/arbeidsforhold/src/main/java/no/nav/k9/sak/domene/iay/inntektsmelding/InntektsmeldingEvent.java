package no.nav.k9.sak.domene.iay.inntektsmelding;

import java.util.Objects;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;

public abstract class InntektsmeldingEvent {

    private final FagsakYtelseType ytelseType;
    private final AktørId aktørId;
    private final JournalpostId journalpostId;

    public InntektsmeldingEvent(FagsakYtelseType ytelseType, AktørId aktørId, JournalpostId journalpostId) {
        this.ytelseType = ytelseType;
        this.aktørId = aktørId;
        this.journalpostId = journalpostId;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        var other = (InntektsmeldingEvent) obj;

        return Objects.equals(ytelseType, other.ytelseType)
            && Objects.equals(aktørId, other.aktørId)
            && Objects.equals(journalpostId, other.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, aktørId, ytelseType);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<ytelseType=" + ytelseType + ", journalpostId=" + journalpostId + ">";
    }

    public static class Mottatt extends InntektsmeldingEvent {

        public Mottatt(FagsakYtelseType ytelseType, AktørId aktørId, JournalpostId journalpostId) {
            super(ytelseType, aktørId, journalpostId);
        }

    }
}
