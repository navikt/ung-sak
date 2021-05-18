package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Entity(name = "UnntakEtablertTilsyn")
@Table(name = "PSB_UNNTAK_ETABLERT_TILSYN")
public class UnntakEtablertTilsyn extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_UNNTAK_ETABLERT_TILSYN")
    private Long id;

    @Immutable
    @OneToMany(mappedBy = "unntakEtablertTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UnntakEtablertTilsynPeriode> perioder = new ArrayList<>();

    @Immutable
    @OneToMany(mappedBy = "unntakEtablertTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UnntakEtablertTilsynBeskrivelse> beskrivelser = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UnntakEtablertTilsyn() {
        // hibernate
    }

    public UnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        Objects.requireNonNull(unntakEtablertTilsyn);
        this.perioder = unntakEtablertTilsyn.getPerioder()
            .stream()
            .map(UnntakEtablertTilsynPeriode::new)
            .peek(it -> it.setUnntakEtablertTilsyn(this))
            .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }


    public List<UnntakEtablertTilsynPeriode> getPerioder() {
        return perioder;
    }

    public void setPerioder(List<UnntakEtablertTilsynPeriode> perioder) {
        this.perioder = perioder.stream()
            .peek(it -> it.setUnntakEtablertTilsyn(this))
            .collect(Collectors.toList());
    }

    public List<UnntakEtablertTilsynBeskrivelse> getBeskrivelser() {
        return beskrivelser;
    }

    public void setBeskrivelser(List<UnntakEtablertTilsynBeskrivelse> beskrivelser) {
        this.beskrivelser = beskrivelser;
    }

    @Override
    public String toString() {
        return "UnntakEtablertTilsyn{" +
            "id=" + id +
            ", perioder=" + perioder +
            ", beskrivelser=" + beskrivelser +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnntakEtablertTilsyn that = (UnntakEtablertTilsyn) o;
        return perioder.equals(that.perioder) && beskrivelser.equals(that.beskrivelser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder, beskrivelser);
    }
}
