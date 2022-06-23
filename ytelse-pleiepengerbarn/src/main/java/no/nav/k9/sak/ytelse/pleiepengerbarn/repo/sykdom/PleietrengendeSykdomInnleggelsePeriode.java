package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
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

@Entity(name = "PleietrengendeSykdomInnleggelsePeriode")
@Table(name = "PLEIETRENGENDE_SYKDOM_INNLEGGELSE_PERIODE")
public class PleietrengendeSykdomInnleggelsePeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM_INNLEGGELSE_PERIODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_INNLEGGELSER_ID")
    private PleietrengendeSykdomInnleggelser innleggelser;

    @Column(name = "FOM", nullable = false)
    private LocalDate fom;

    @Column(name = "TOM", nullable = false)
    private LocalDate tom;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomInnleggelsePeriode() {
        //HIBERNATE
    }

    public PleietrengendeSykdomInnleggelsePeriode(LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.fom = fom;
        this.tom = tom;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public PleietrengendeSykdomInnleggelsePeriode(PleietrengendeSykdomInnleggelser innleggelser, LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this(fom, tom, opprettetAv, opprettetTidspunkt);
        this.innleggelser = innleggelser;
    }

    public Long getId() {
        return id;
    }

    public PleietrengendeSykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(PleietrengendeSykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
