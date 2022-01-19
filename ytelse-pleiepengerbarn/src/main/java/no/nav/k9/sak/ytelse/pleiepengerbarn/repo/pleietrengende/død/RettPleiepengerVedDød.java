package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.kodeverk.uttak.RettVedDødType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

import jakarta.persistence.*;
import java.util.Objects;

@Entity(name = "RettVedPleietrengendeDød")
@Table(name = "psb_rett_pleiepenger_ved_doed")
public class RettPleiepengerVedDød extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_psb_rett_pleiepenger_ved_doed")
    private Long id;

    @Column(name = "vurdering")
    private String vurdering;

    @Convert(converter = RettVedDødTypeConverter.class)
    @Column(name = "rett_ved_doed_type")
    private RettVedDødType rettVedDødType;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public RettPleiepengerVedDød() {
        // Hibernate
    }

    public RettPleiepengerVedDød(String vurdering, RettVedDødType rettVedDødType) {
        this.vurdering = vurdering;
        this.rettVedDødType = rettVedDødType;
    }

    public Long getId() {
        return id;
    }

    public String getVurdering() {
        return vurdering;
    }

    public RettVedDødType getRettVedDødType() {
        return rettVedDødType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RettPleiepengerVedDød that = (RettPleiepengerVedDød) o;
        return vurdering.equals(that.vurdering) && rettVedDødType == that.rettVedDødType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurdering, rettVedDødType);
    }

    @Override
    public String toString() {
        return "RettVedPleietrengendeDød{" +
            "id=" + id +
            ", rettVedDødType=" + rettVedDødType +
            '}';
    }
}
