package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "SykdomVurderinger")
@Table(name = "SYKDOM_VURDERINGER")
public class SykdomVurderinger {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_VURDERINGER")
    private Long id;

    @OneToOne
    @JoinColumn(name = "SYK_PERSON_ID", nullable = false)
    private SykdomPerson person;

    @OneToMany(mappedBy = "sykdomVurderinger") //TODO: cascades? , cascade = {CascadeType.PERSIST, CascadeType.REFRESH}
    private List<SykdomVurdering> vurderinger = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomVurderinger() {
        // hibernate
    }

    public SykdomVurderinger(
            SykdomPerson person,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.person = person;
        this.vurderinger = new ArrayList<>();
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public Long getId() {
        return id;
    }

    public SykdomPerson getPerson() {
        return person;
    }

    public void setPerson(SykdomPerson person) {
        this.person = person;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public SykdomVurdering getSisteVurdering() {
        return vurderinger.stream().max(Comparator.naturalOrder()).orElse(null);
    }
}
