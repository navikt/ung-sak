package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

//TODO: SykdomDiagnose
@Entity(name = "PleietrengendeSykdomDiagnose")
@Table(name = "PLEIETRENGENDE_SYKDOM_DIAGNOSE")
public class PleietrengendeSykdomDiagnose {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM_DIAGNOSE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_DIAGNOSER_ID")
    private PleietrengendeSykdomDiagnoser diagnoser;

    @Column(name = "DIAGNOSEKODE", nullable = false, updatable=false)
    private String diagnosekode;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomDiagnose() {
        //Hibernate
    }

    public PleietrengendeSykdomDiagnose(String diagnosekode, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.diagnosekode = diagnosekode;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public PleietrengendeSykdomDiagnose(PleietrengendeSykdomDiagnoser diagnoser, String diagnosekode, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this(diagnosekode, opprettetAv, opprettetTidspunkt);
        this.diagnoser = diagnoser;
    }

    public PleietrengendeSykdomDiagnoser getDiagnoser() {
        return diagnoser;
    }

    public void setDiagnoser(PleietrengendeSykdomDiagnoser diagnosekoder) {
        this.diagnoser = diagnosekoder;
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
