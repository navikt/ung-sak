package no.nav.ung.sak.behandlingslager.grunnbeløp;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "GrunnbeløpSats")
@Table(name = "GRUNNBELOP_SATS")
public class GrunnbeløpSats {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GRUNNBELOP_SATS")
    private Long id;

    @Embedded
    private DatoIntervallEntitet periode;

    @Column(name = "verdi", nullable = false, updatable = false)
    private long verdi;

    public Long getId() {
        return id;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public long getVerdi() {
        return verdi;
    }
}
