package no.nav.ung.sak.domene.iay.modell;

import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Embeddable;

import no.nav.k9.kodeverk.arbeidsforhold.BekreftetPermisjonStatus;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@Embeddable
public class BekreftetPermisjon {

    private BekreftetPermisjonStatus status = BekreftetPermisjonStatus.UDEFINERT;

    private DatoIntervallEntitet periode;

    BekreftetPermisjon() {
    }

    public BekreftetPermisjon(BekreftetPermisjonStatus status){
        this.status = status;
    }

    public BekreftetPermisjon(LocalDate permisjonFom, LocalDate permisjonTom, BekreftetPermisjonStatus status){
        this.periode = DatoIntervallEntitet.fraOgMedTilOgMed(permisjonFom, permisjonTom);
        this.status = status;
    }

    public BekreftetPermisjon(BekreftetPermisjon bekreftetPermisjon) {
        this.periode = bekreftetPermisjon.getPeriode();
        this.status = bekreftetPermisjon.getStatus();
    }

    public BekreftetPermisjonStatus getStatus() {
        return status;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof BekreftetPermisjon))
            return false;
        BekreftetPermisjon that = (BekreftetPermisjon) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, status);
    }

    @Override
    public String toString() {
        return "BekreftetPermisjon<" +
            "periode=" + periode +
            ", status=" + status +
            '>';
    }

}
