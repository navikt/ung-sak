package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "SykdomInnleggelsePeriode")
@Table(name = "SYKDOM_INNLEGGELSE_PERIODE")
class SykdomInnleggelsePeriode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_INNLEGGELSE_PERIODE")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_INNLEGGELSE_ID")
    private SykdomInnleggelse innleggelse;

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

    SykdomInnleggelsePeriode() {
        //HIBERNATE
    }

    public SykdomInnleggelsePeriode(SykdomInnleggelse innleggelse, LocalDate fom, LocalDate tom, String opprettetAv, LocalDateTime opprettetTidspunkt) {
        this.innleggelse = innleggelse;
        this.fom = fom;
        this.tom = tom;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public Long getId() {
        return id;
    }

    public SykdomInnleggelse getInnleggelse() {
        return innleggelse;
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
