package no.nav.ung.sak.behandlingslager.behandling.søknadsperiode;

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
import no.nav.ung.sak.BaseEntitet;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.JournalpostId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity(name = "AktivitetspengerSøktPeriode")
@Table(name = "akt_soekt_periode")
public class AktivitetspengerSøktPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_akt_soekt_periode")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "journalpost_id", nullable = false, updatable = false)
    private String journalpostId;

    @Column(name = "journalpost_mottatt_tid", nullable = false, updatable = false)
    private LocalDateTime journalpostMottattTid;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    protected AktivitetspengerSøktPeriode() {
        //brukes av JPA/Hibernate
    }

    public AktivitetspengerSøktPeriode(Long behandlingId, JournalpostId journalpostId, LocalDateTime journalpostMottattTid, DatoIntervallEntitet periode) {
        this.behandlingId = behandlingId;
        this.journalpostId = journalpostId.getVerdi();
        this.journalpostMottattTid = journalpostMottattTid;
        this.periode = periode;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || (o instanceof AktivitetspengerSøktPeriode that)
            && Objects.equals(behandlingId, that.behandlingId)
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(periode, that.periode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, journalpostId, periode);
    }
}
