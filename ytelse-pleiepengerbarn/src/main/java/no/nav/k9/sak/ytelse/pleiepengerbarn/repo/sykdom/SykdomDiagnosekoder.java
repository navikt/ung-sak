package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "SykdomDiagnosekoder")
@Table(name = "SYKDOM_DIAGNOSEKODER")
public class SykdomDiagnosekoder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DIAGNOSEKODER")
    private Long id;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @OneToMany(mappedBy = "diagnosekode", cascade = CascadeType.PERSIST)
    private List<SykdomDiagnosekode> diagnosekoder;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomDiagnosekoder() {
        // hibernate
    }

    public SykdomDiagnosekoder(
            Long versjon,
            List<SykdomDiagnosekode> diagnosekoder,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.versjon = versjon;
        this.diagnosekoder = diagnosekoder;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public Long getVersjon() {
        return versjon;
    }

    public List<SykdomDiagnosekode> getDiagnosekoder() {
        return diagnosekoder;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
