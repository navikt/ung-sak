package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.time.LocalDate;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "UttakNyeRegler")
@Table(name = "UTTAK_NYE_REGLER")
@DynamicUpdate
public class UttakNyeRegler extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAK_NYE_REGLER")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @Column(name = "virkningsdato", updatable = false)
    private LocalDate virkningsdato;

    protected UttakNyeRegler() {
        //for hibernate
    }

    public UttakNyeRegler(Long behandlingId, LocalDate virkningsdato) {
        this.behandlingId = behandlingId;
        this.virkningsdato = virkningsdato;
    }

    public Long getId() {
        return id;
    }

    public void deaktiver() {
        aktiv = false;
    }

    public LocalDate getVirkningsdato() {
        return virkningsdato;
    }
}

