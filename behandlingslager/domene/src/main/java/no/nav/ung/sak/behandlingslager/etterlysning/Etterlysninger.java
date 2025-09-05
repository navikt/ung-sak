package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.BatchSize;

import java.util.List;

@Entity(name = "Etterlysninger")
@Table(name = "ETTERLYSNINGER")
public class Etterlysninger extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETTERLYSNINGER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "etterlysninger_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private List<Etterlysning> etterlysninger;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Etterlysninger(List<Etterlysning> etterlysninger) {
        this.etterlysninger = etterlysninger;
    }

    public Etterlysninger() {
    }

    public List<Etterlysning> getEtterlysninger() {
        return etterlysninger;
    }
}
