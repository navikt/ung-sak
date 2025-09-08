package no.nav.ung.sak.behandlingslager.etterlysning;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.Immutable;

import java.util.List;
import java.util.Objects;

@Entity(name = "ForhåndsvarselGrunnlag")
@Table(name = "GR_FORHANDSVARSEL")
public class ForhåndsvarselGrunnlagEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_FORHANDSVARSEL")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @Immutable
    @JoinColumn(name = "varslinger_id", nullable = false, updatable = false, unique = true)
    private Etterlysninger etterlysninger;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;


    ForhåndsvarselGrunnlagEntitet() {
    }

    ForhåndsvarselGrunnlagEntitet(Long behandlingId, Etterlysninger etterlysninger) {
        this.behandlingId = behandlingId;
        this.etterlysninger = etterlysninger;
    }

    ForhåndsvarselGrunnlagEntitet(Etterlysninger etterlysninger) {
        this.etterlysninger = etterlysninger;
    }

    Etterlysninger getEtterlysningerEnitet() {
        return etterlysninger;
    }

    public List<Etterlysning> getEtterlysninger() {
        return etterlysninger.getEtterlysninger();
    }

    Long getId() {
        return id;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForhåndsvarselGrunnlagEntitet that = (ForhåndsvarselGrunnlagEntitet) o;
        return Objects.equals(etterlysninger, that.etterlysninger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(etterlysninger);
    }

}
