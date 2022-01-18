package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "SykdomDokumentHarOppdatertEksisterendeVurderinger")
@Table(name = "SYKDOM_DOKUMENT_HAR_OPPDATERT_EKSISTERENDE_VURDERINGER")
public class SykdomDokumentHarOppdatertEksisterendeVurderinger implements Serializable {

    @Id
    @Column(name = "SYKDOM_DOKUMENT_ID", unique = true, nullable = false)
    private Long id;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomDokumentHarOppdatertEksisterendeVurderinger() {
        // hibernate
    }

    public SykdomDokumentHarOppdatertEksisterendeVurderinger(SykdomDokument dokument, String opprettetAv, LocalDateTime opprettetTidspunkt) {
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
