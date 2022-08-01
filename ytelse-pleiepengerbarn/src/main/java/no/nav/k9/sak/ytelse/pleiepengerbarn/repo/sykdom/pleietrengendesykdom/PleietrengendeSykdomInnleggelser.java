package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "PleietrengendeSykdomInnleggelser")
@Table(name = "PLEIETRENGENDE_SYKDOM_INNLEGGELSER")
public class PleietrengendeSykdomInnleggelser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM_INNLEGGELSER")
    private Long id;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_ID" )
    private PleietrengendeSykdom pleietrengendeSykdom;

    @OneToMany(mappedBy = "innleggelser", cascade = CascadeType.PERSIST)
    private List<PleietrengendeSykdomInnleggelsePeriode> perioder = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomInnleggelser() {
        // hibernate
    }

    public PleietrengendeSykdomInnleggelser(
            Long versjon,
            List<PleietrengendeSykdomInnleggelsePeriode> perioder,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.versjon = versjon;
        this.perioder = perioder.stream()
            .map(p -> {
                if(p.getInnleggelser() != null && p.getInnleggelser() != this) {
                    throw new IllegalStateException("Potensiell krysskobling av perioder fra andre innleggelser!");
                }
                p.setInnleggelser(this);
                return p;
            }).collect(Collectors.toCollection(ArrayList::new));
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public PleietrengendeSykdomInnleggelser(
        Long versjon,
        PleietrengendeSykdom pleietrengendeSykdom,
        List<PleietrengendeSykdomInnleggelsePeriode> perioder,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this(versjon, perioder, opprettetAv, opprettetTidspunkt);
        this.pleietrengendeSykdom = pleietrengendeSykdom;
    }

    public Long getId() {
        return id;
    }

    public Long getVersjon() {
        return versjon;
    }

    public void setVersjon(Long versjon) {
        this.versjon = versjon;
    }

    public PleietrengendeSykdom getPleietrengendeSykdom() {
        return pleietrengendeSykdom;
    }

    public void setPleietrengendeSykdom(PleietrengendeSykdom vurderinger) {
        this.pleietrengendeSykdom = vurderinger;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void leggTilPeriode(PleietrengendeSykdomInnleggelsePeriode periode) {
        if(periode.getInnleggelser() != null && periode.getInnleggelser() != this) {
            throw new IllegalStateException("Potensiell krysskobling av perioder fra andre innleggelser!");
        }
        periode.setInnleggelser(this);
        perioder.add(periode);
    }

    public List<PleietrengendeSykdomInnleggelsePeriode> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }
}
