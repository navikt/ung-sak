package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType.SykdomVurderingTypeConverter;

@Entity(name = "SykdomInnleggelse")
@Table(name = "SYKDOM_INNLEGGELSE")
public class SykdomInnleggelse {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_INNLEGGELSE")
    private Long id;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @OneToMany(mappedBy = "innleggelse", cascade = CascadeType.PERSIST)
    private List<SykdomInnleggelsePeriode> perioder;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomInnleggelse() {
        // hibernate
    }

    public SykdomInnleggelse(
            Long versjon,
            List<SykdomInnleggelsePeriode> perioder,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
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

    public List<SykdomInnleggelsePeriode> getPerioder() {
        return perioder;
    }
}
