package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.List;

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
import javax.persistence.Version;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomVurderingType.SykdomVurderingTypeConverter;

@Entity(name = "SykdomVurdering")
@Table(name = "SYKDOM_VURDERING")
public class SykdomVurdering {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING")
    private Long id;

    @Column(name = "TYPE", nullable = false)
    @Convert(converter = SykdomVurderingTypeConverter.class)
    private SykdomVurderingType type;

    @Column(name = "RANGERING")
    private Long rangering;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private SykdomVurderinger sykdomVurderinger;

    @OneToMany(mappedBy = "sykdomVurdering")
    private List<SykdomVurderingVersjon> sykdomVurderingVersjoner;

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
            List<SykdomVurderingVersjon> sykdomVurderingVersjoner,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.type = type;
        this.rangering = rangering;
        this.sykdomVurderinger = sykdomVurderinger;
        this.sykdomVurderingVersjoner = sykdomVurderingVersjoner;
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

    public List<SykdomVurderingVersjon> getSykdomVurderingVersjoner() {
        return sykdomVurderingVersjoner;
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
