package no.nav.ung.sak.behandlingslager.behandling.part;

import jakarta.persistence.*;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.felles.typer.IdType;
import no.nav.ung.sak.felles.typer.Identifikasjon;
import no.nav.ung.sak.felles.typer.RolleType;

import java.util.Objects;

@Table(name = "PART")
@Entity(name = "PartEntitet")
public class PartEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PART")
    private Long id;

    @Column(name = "identifikasjon", nullable = false)
    public String identifikasjon;

    @Column(name = "identifikasjon_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public IdType identifikasjonType;

    @Column(name = "rolle_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public RolleType rolleType;

    public PartEntitet() {
        // Hibernate
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PartEntitet partEntitet = (PartEntitet) o;
        return Objects.equals(id, partEntitet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    public Identifikasjon getIdentifikasjon() {
        return new Identifikasjon(identifikasjon, identifikasjonType);
    }
}
