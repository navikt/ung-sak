package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Person;

//Døpe om til SykdomPleietrengendeInformasjon
@Entity(name = "PleietrengendeSykdom")
@Table(name = "PLEIETRENGENDE_SYKDOM")
public class PleietrengendeSykdom {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM")
    private Long id;

    @OneToOne
    @JoinColumn(name = "PLEIETRENGENDE_PERSON_ID", nullable = false)
    private Person person;

    @OneToMany(mappedBy = "pleietrengendeSykdom") //TODO: cascades? , cascade = {CascadeType.PERSIST, CascadeType.REFRESH}
    private List<PleietrengendeSykdomVurdering> vurderinger = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdom() {
        // hibernate
    }

    public PleietrengendeSykdom(
            Person person,
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

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public PleietrengendeSykdomVurdering getSisteVurdering() {
        return vurderinger.stream().max(Comparator.naturalOrder()).orElse(null);
    }
}
