package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.unntaketablerttilsyn;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(name = "UnntakEtablertTilsyn")
@Table(name = "psb_unntak_etablert_tilsyn")
public class UnntakEtablertTilsyn extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PSB_UNNTAK_ETABLERT_TILSYN")
    private Long id;

    @OneToMany(mappedBy = "unntakEtablertTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UnntakEtablertTilsynPeriode> perioder = new ArrayList<>();

    @OneToMany(mappedBy = "unntakEtablertTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UnntakEtablertTilsynBeskrivelse> beskrivelser = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UnntakEtablertTilsyn() {
        // hibernate
    }

    public UnntakEtablertTilsyn(List<UnntakEtablertTilsynPeriode> perioder, List<UnntakEtablertTilsynBeskrivelse> beskrivelser) {
        this.perioder = perioder
            .stream()
            .map(periode -> new UnntakEtablertTilsynPeriode(periode).medUnntakEtablertTilsyn(this))
            .toList();
        this.beskrivelser = beskrivelser
            .stream()
            .map(beskrivelse -> new UnntakEtablertTilsynBeskrivelse(beskrivelse).medUnntakEtablertTilsyn(this))
            .toList();
    }

    public UnntakEtablertTilsyn(UnntakEtablertTilsyn unntakEtablertTilsyn) {
        this(unntakEtablertTilsyn.perioder, unntakEtablertTilsyn.getBeskrivelser());
    }

    public Long getId() {
        return id;
    }

    public List<UnntakEtablertTilsynPeriode> getPerioder() {
        return perioder;
    }

    public List<UnntakEtablertTilsynBeskrivelse> getBeskrivelser() {
        return beskrivelser;
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
