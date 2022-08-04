package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

//Objektet brukes for at saksbehandler skal kvittere ut at de har sjekket alle eksisterende
// vurderinger på den pleietrengende når et nytt dokument kommer til.
@Entity(name = "PleietrengendeSykdomDokumentHarOppdatertVurderinger")
@Table(name = "PLEIETRENGENDE_SYKDOM_DOKUMENT_HAR_OPPDATERT_VURDERINGER")
public class PleietrengendeSykdomDokumentHarOppdatertVurderinger implements Serializable {

    @Id
    @Column(name = "PLEIETRENGENDE_SYKDOM_DOKUMENT_ID", unique = true, nullable = false)
    private Long id;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomDokumentHarOppdatertVurderinger() {
        // hibernate
    }

    public PleietrengendeSykdomDokumentHarOppdatertVurderinger(PleietrengendeSykdomDokument dokument, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        if (dokument.getId() == null) {
            throw new IllegalArgumentException("Kan ikke utkvittere dokumenter som ikke er peristert først");
        }

        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
        this.id = dokument.getId();
    }

    public Long getId() {
        return id;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
