package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

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

@Entity(name = "SykdomInnleggelser")
@Table(name = "SYKDOM_INNLEGGELSER")
public class SykdomInnleggelser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_INNLEGGELSER")
    private Long id;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID" )
    private SykdomVurderinger vurderinger;

    @OneToMany(mappedBy = "innleggelser", cascade = CascadeType.PERSIST)
    private List<SykdomInnleggelsePeriode> perioder = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomInnleggelser() {
        // hibernate
    }

    public SykdomInnleggelser(
            Long versjon,
            List<SykdomInnleggelsePeriode> perioder,
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

    public SykdomInnleggelser(
        Long versjon,
        SykdomVurderinger vurderinger,
        List<SykdomInnleggelsePeriode> perioder,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this(versjon, perioder, opprettetAv, opprettetTidspunkt);
        this.vurderinger = vurderinger;
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

    public SykdomVurderinger getVurderinger() {
        return vurderinger;
    }

    public void setVurderinger(SykdomVurderinger vurderinger) {
        this.vurderinger = vurderinger;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void leggTilPeriode(SykdomInnleggelsePeriode periode) {
        if(periode.getInnleggelser() != null && periode.getInnleggelser() != this) {
            throw new IllegalStateException("Potensiell krysskobling av perioder fra andre innleggelser!");
        }
        periode.setInnleggelser(this);
        perioder.add(periode);
    }

    public List<SykdomInnleggelsePeriode> getPerioder() {
        return Collections.unmodifiableList(perioder);
    }
}
