package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "SykdomVurdering")
@Table(name = "SYKDOM_VURDERING")
public class SykdomVurdering {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING")
    private Long id;

    @Column(name = "TYPE", nullable = false)
    private SykdomVurderingType type;

    @Column(name = "RANGERING")
    private Long rangering;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private SykdomVurderinger sykdomVurderinger;

    @OneToOne
    @JoinColumn(name = "SYKDOM_VURDERING_ID", nullable = false)
    private SykdomVurderingVersjon sykdomVurderingVersjon;

    @Version
    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomVurdering() {
        // hibernate
    }

    public SykdomVurdering(
            SykdomVurderingType type,
            Long rangering,
            SykdomVurderinger sykdomVurderinger,
            SykdomVurderingVersjon sykdomVurderingVersjon,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.type = type;
        this.rangering = rangering;
        this.sykdomVurderinger = sykdomVurderinger;
        this.sykdomVurderingVersjon = sykdomVurderingVersjon;
        this.versjon = versjon;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public Long getId() {
        return id;
    }

    public SykdomVurderingType getType() {
        return type;
    }

    public Long getRangering() {
        return rangering;
    }

    public SykdomVurderinger getSykdomVurderinger() {
        return sykdomVurderinger;
    }

    public void setSykdomVurderinger(SykdomVurderinger sykdomVurderinger) {
        this.sykdomVurderinger = sykdomVurderinger;
    }

    public SykdomVurderingVersjon getSykdomVurderingVersjon() {
        return sykdomVurderingVersjon;
    }

    public Long getVersjon() {
        return versjon;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }
}
