package no.nav.k9.sak.domene.iay.modell;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;


public class OppgittFrilans {

    private OppgittOpptjening oppgittOpptjening;

    private boolean harInntektFraFosterhjem;

    private boolean erNyoppstartet;

    private boolean harNærRelasjon;

    @ChangeTracked
    private List<OppgittFrilansoppdrag> frilansoppdrag;

    public OppgittFrilans() {
    }

    public OppgittFrilans(boolean harInntektFraFosterhjem, boolean erNyoppstartet, boolean harNærRelasjon) {
        this.harInntektFraFosterhjem = harInntektFraFosterhjem;
        this.erNyoppstartet = erNyoppstartet;
        this.harNærRelasjon = harNærRelasjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof OppgittFrilans)) return false;
        OppgittFrilans that = (OppgittFrilans) o;
        return harInntektFraFosterhjem == that.harInntektFraFosterhjem &&
            erNyoppstartet == that.erNyoppstartet &&
            harNærRelasjon == that.harNærRelasjon &&
            Objects.equals(oppgittOpptjening, that.oppgittOpptjening) &&
            Objects.equals(frilansoppdrag, that.frilansoppdrag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittOpptjening, harInntektFraFosterhjem, erNyoppstartet, harNærRelasjon, frilansoppdrag);
    }

    @Override
    public String toString() {
        return "FrilansEntitet{" +
            "oppgittOpptjening=" + oppgittOpptjening +
            ", harInntektFraFosterhjem=" + harInntektFraFosterhjem +
            ", erNyoppstartet=" + erNyoppstartet +
            ", harNærRelasjon=" + harNærRelasjon +
            ", frilansoppdrag=" + frilansoppdrag +
            '}';
    }

    void setOppgittOpptjening(OppgittOpptjening oppgittOpptjening) {
        this.oppgittOpptjening = oppgittOpptjening;
    }

    public boolean getHarInntektFraFosterhjem() {
        return harInntektFraFosterhjem;
    }

    // FIXME (OJR) kan ikke ha mutators
    public void setHarInntektFraFosterhjem(boolean harInntektFraFosterhjem) {
        this.harInntektFraFosterhjem = harInntektFraFosterhjem;
    }

    // FIXME (OJR) kan ikke ha mutators
    public void setErNyoppstartet(boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

    // FIXME (OJR) kan ikke ha mutators
    public void setHarNærRelasjon(boolean harNærRelasjon) {
        this.harNærRelasjon = harNærRelasjon;
    }

    public boolean getErNyoppstartet() {
        return erNyoppstartet;
    }

    public boolean getHarNærRelasjon() {
        return harNærRelasjon;
    }

    public List<OppgittFrilansoppdrag> getFrilansoppdrag() {
        if (frilansoppdrag != null) {
            return Collections.unmodifiableList(frilansoppdrag);
        }
        return Collections.emptyList();
    }

    public void setFrilansoppdrag(List<OppgittFrilansoppdrag> frilansoppdrag) {
        this.frilansoppdrag = frilansoppdrag;
    }
}
