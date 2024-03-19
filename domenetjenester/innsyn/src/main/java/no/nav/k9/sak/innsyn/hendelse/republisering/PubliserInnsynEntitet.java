package no.nav.k9.sak.innsyn.hendelse.republisering;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;



/**
 * En rad i arbeidstabell for publisering av behandlinger til innsyn
 */
@Entity(name = "PubliserInnsynEntitet")
@Table(name = "publiser_innsyn_arbeidstabell")
public class PubliserInnsynEntitet extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_publiser_innsyn_arbeidstabell")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private long behandlingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", updatable = true, nullable = false)
    private Status status;

    @Column(name = "endring", updatable = true, nullable = true)
    private String endring;

    @Column(name = "kjøring_id", updatable = false, nullable = false)
    private UUID kjøringUuid;

    public PubliserInnsynEntitet() {
    }

    public PubliserInnsynEntitet(long behandlingId, UUID kjøringUuid) {
        this.behandlingId = behandlingId;
        this.status = Status.NY;
        this.kjøringUuid = kjøringUuid;
    }

    void fullført() {
        this.status = Status.FULLFØRT;
    }

    void feilet(String feilmelding) {
        this.status = Status.FEILET;
        this.endring = feilmelding;
    }

    public long getBehandlingId() {
        return behandlingId;
    }

    public Long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        NY, FULLFØRT, FEILET, KANSELLERT
    }
}


