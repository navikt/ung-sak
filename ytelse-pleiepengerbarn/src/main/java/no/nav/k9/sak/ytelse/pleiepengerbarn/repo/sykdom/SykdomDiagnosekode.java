package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import net.bytebuddy.asm.Advice;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "SykdomDiagnosekode")
@Table(name = "SYKDOM_DIAGNOSEKODE")
public class SykdomDiagnosekode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DIAGNOSEKODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_DIAGNOSEKODER_ID")
    private SykdomDiagnosekoder diagnosekoder;

    @Column(name = "DIAGNOSEKODE", nullable = false, updatable=false)
    private String diagnosekode;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;


    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomDiagnosekode() {
        //Hibernate
    }

    public SykdomDiagnosekode(String diagnosekode, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.diagnosekode = diagnosekode;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public SykdomDiagnosekode(SykdomDiagnosekoder diagnosekoder, String diagnosekode, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this(diagnosekode, opprettetAv, opprettetTidspunkt);
        this.diagnosekoder = diagnosekoder;
    }

    public SykdomDiagnosekoder getDiagnosekoder() {
        return diagnosekoder;
    }

    public void setDiagnosekoder(SykdomDiagnosekoder diagnosekoder) {
        this.diagnosekoder = diagnosekoder;
    }

    public String getDiagnosekode() {
        return diagnosekode;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
