package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "OppgittUtenlandsopphold")
@Table(name = "OPPGITT_FMEDLEMSKAP_UTENLANDSOPPHOLD")
@Immutable
public class OppgittUtenlandsopphold extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPGITT_FMEDLEMSKAP_UTENLANDSOPPHOLD")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "landkode", nullable = false, updatable = false)
    private String landkode;

    @Column(name = "har_jobbet_i_perioden", nullable = false, updatable = false)
    private boolean harJobbetIPerioden;

    @Column(name = "utenlandsk_nasjonal_id", updatable = false)
    private String utenlandskNasjonalId;

    public OppgittUtenlandsopphold() {

    }

    public OppgittUtenlandsopphold(LocalDate fom, LocalDate tom, String landkode, boolean harJobbetIPerioden, String utenlandsNasjonalId) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        Objects.requireNonNull(landkode, "landkode");

        this.periode = Range.closed(fom, tom);
        this.landkode = landkode;
        this.harJobbetIPerioden = harJobbetIPerioden;
        this.utenlandskNasjonalId = utenlandsNasjonalId;
    }

    OppgittUtenlandsopphold(OppgittUtenlandsopphold other) {
        this.periode = other.periode;
        this.landkode = other.landkode;
        this.harJobbetIPerioden = other.harJobbetIPerioden;
        this.utenlandskNasjonalId = other.utenlandskNasjonalId;
    }

    public Long getId() {
        return id;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public String getLandkode() {
        return landkode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittUtenlandsopphold that = (OppgittUtenlandsopphold) o;
        return Objects.equals(getPeriode(), that.getPeriode())
            && Objects.equals(landkode, that.landkode)
            && Objects.equals(harJobbetIPerioden, that.harJobbetIPerioden)
            && Objects.equals(utenlandskNasjonalId, that.utenlandskNasjonalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPeriode(), landkode, harJobbetIPerioden, utenlandskNasjonalId);
    }

    public boolean harJobbetIPerioden() {
        return harJobbetIPerioden;
    }

    public String getUtenlandskNasjonalId() {
        return utenlandskNasjonalId;
    }

    @Override
    public String toString() {
        return "OppgittUtenlandsopphold{" +
            "periode=" + periode +
            ", landkode='" + landkode + '\'' +
            ", harJobbetIPerioden=" + harJobbetIPerioden +
            ", utenlandskNasjonalId='" + (utenlandskNasjonalId != null) + '\'' +
            '}';
    }
}
