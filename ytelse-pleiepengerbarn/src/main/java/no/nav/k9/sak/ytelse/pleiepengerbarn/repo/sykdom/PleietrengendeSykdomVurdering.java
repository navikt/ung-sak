package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.SykdomVurderingType;

@Entity(name = "PleietrengendeSykdomVurdering")
@Table(name = "PLEIETRENGENDE_SYKDOM_VURDERING")
public class PleietrengendeSykdomVurdering implements Comparable<PleietrengendeSykdomVurdering> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERING")
    private Long id;

    @Column(name = "TYPE", nullable = false)
    @Convert(converter = SykdomVurderingTypeConverter.class)
    private SykdomVurderingType type;

    @Column(name = "RANGERING")
    private Long rangering;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private PleietrengendeSykdom pleietrengendeSykdom;

    @OneToMany(mappedBy = "pleietrengendeSykdomVurdering", cascade = CascadeType.PERSIST)
    private List<PleietrengendeSykdomVurderingVersjon> pleietrengendeSykdomVurderingVersjoner;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomVurdering() {
        // hibernate
    }

    public PleietrengendeSykdomVurdering(
            SykdomVurderingType type,
            List<PleietrengendeSykdomVurderingVersjon> pleietrengendeSykdomVurderingVersjoner,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.type = type;
        this.pleietrengendeSykdomVurderingVersjoner = new ArrayList<>(pleietrengendeSykdomVurderingVersjoner);
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

    public void setRangering(Long rangering) {
        this.rangering = rangering;
    }

    public PleietrengendeSykdom getSykdomVurderinger() {
        return pleietrengendeSykdom;
    }

    void setSykdomVurderinger(PleietrengendeSykdom pleietrengendeSykdom) {
        this.pleietrengendeSykdom = pleietrengendeSykdom;
    }

    public List<PleietrengendeSykdomVurderingVersjon> getSykdomVurderingVersjoner() {
        return Collections.unmodifiableList(pleietrengendeSykdomVurderingVersjoner);
    }

    public void addVersjon(PleietrengendeSykdomVurderingVersjon versjon) {
        this.pleietrengendeSykdomVurderingVersjoner.add(versjon);
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public PleietrengendeSykdomVurderingVersjon getSisteVersjon() {
        return pleietrengendeSykdomVurderingVersjoner.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    @Override
    public int compareTo(PleietrengendeSykdomVurdering v2) {
        return getRangering().compareTo(v2.getRangering());
    }
}
