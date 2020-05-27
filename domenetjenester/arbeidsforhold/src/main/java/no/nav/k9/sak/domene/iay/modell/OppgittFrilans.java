package no.nav.k9.sak.domene.iay.modell;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

public class OppgittFrilans {

    private Boolean harInntektFraFosterhjem;

    private Boolean erNyoppstartet;

    private Boolean harNærRelasjon;

    @ChangeTracked
    private List<OppgittFrilansoppdrag> frilansoppdrag;

    public OppgittFrilans() {
    }

    public OppgittFrilans(OppgittFrilans kopierFra) {
        this.harInntektFraFosterhjem = kopierFra.harInntektFraFosterhjem;
        this.erNyoppstartet = kopierFra.erNyoppstartet;
        this.harNærRelasjon = kopierFra.harNærRelasjon;
        this.setFrilansoppdrag(kopierFra.getFrilansoppdrag().stream().map(OppgittFrilansoppdrag::new).collect(Collectors.toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittFrilans))
            return false;
        OppgittFrilans that = (OppgittFrilans) o;
        return Objects.equals(harInntektFraFosterhjem, that.harInntektFraFosterhjem)
            && Objects.equals(erNyoppstartet, that.erNyoppstartet)
            && Objects.equals(harNærRelasjon, that.harNærRelasjon)
            && Objects.equals(frilansoppdrag, that.frilansoppdrag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(harInntektFraFosterhjem, erNyoppstartet, harNærRelasjon, frilansoppdrag);
    }

    @Override
    public String toString() {
        return "FrilansEntitet<" +
            "harInntektFraFosterhjem=" + harInntektFraFosterhjem +
            ", erNyoppstartet=" + erNyoppstartet +
            ", harNærRelasjon=" + harNærRelasjon +
            ", frilansoppdrag=" + frilansoppdrag +
            '>';
    }

    public Boolean getHarInntektFraFosterhjem() {
        return harInntektFraFosterhjem;
    }

    void setHarInntektFraFosterhjem(Boolean harInntektFraFosterhjem) {
        this.harInntektFraFosterhjem = harInntektFraFosterhjem;
    }

    void setErNyoppstartet(Boolean erNyoppstartet) {
        this.erNyoppstartet = erNyoppstartet;
    }

    void setHarNærRelasjon(Boolean harNærRelasjon) {
        this.harNærRelasjon = harNærRelasjon;
    }

    public Boolean getErNyoppstartet() {
        return erNyoppstartet;
    }

    public Boolean getHarNærRelasjon() {
        return harNærRelasjon;
    }

    public List<OppgittFrilansoppdrag> getFrilansoppdrag() {
        if (frilansoppdrag != null) {
            return Collections.unmodifiableList(frilansoppdrag);
        }
        return Collections.emptyList();
    }

    void setFrilansoppdrag(List<OppgittFrilansoppdrag> frilansoppdrag) {
        this.frilansoppdrag = frilansoppdrag;
    }

    void leggTilFrilansoppdrag(OppgittFrilansoppdrag frilansoppdrag) {
        this.frilansoppdrag.add(frilansoppdrag);
    }
}
