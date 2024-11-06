package no.nav.k9.sak.ytelse.ung.søknadsperioder;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VurdertSøktPeriode.SøktPeriodeData;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "UngdomsytelseSøknadsperiode")
@Table(name = "UNG_SOEKNADSPERIODE")
@Immutable
public class UngdomsytelseSøknadsperiode extends BaseEntitet implements SøktPeriodeData {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SOEKNADSPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSøknadsperiode(DatoIntervallEntitet periode, JournalpostId journalpostId) {
        this.periode = periode;
        this.journalpostId = journalpostId;
    }

    public UngdomsytelseSøknadsperiode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }


    public UngdomsytelseSøknadsperiode(LocalDate fom, LocalDate tom, JournalpostId journalpostId) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), journalpostId);
    }

    public UngdomsytelseSøknadsperiode(UngdomsytelseSøknadsperiode it) {
        this.journalpostId = it.getJournalpostId();
        this.periode = it.getPeriode();
    }

    public UngdomsytelseSøknadsperiode() {
        // hibernate
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @Override
    public <V> V getPayload() {
        // skal returnere data til bruk ved komprimering av perioder (dvs. uten periode)
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseSøknadsperiode that = (UngdomsytelseSøknadsperiode) o;
        return Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode);
    }

    @Override
    public String toString() {
        return "UngdomsytelseSøknadsperiode{" +
                "id=" + id +
                ", periode=" + periode +
                ", journalpostId=" + journalpostId +
                ", versjon=" + versjon +
                '}';
    }
}
