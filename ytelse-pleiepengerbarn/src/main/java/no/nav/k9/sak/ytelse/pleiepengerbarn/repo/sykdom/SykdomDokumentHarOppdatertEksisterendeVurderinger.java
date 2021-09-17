package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
