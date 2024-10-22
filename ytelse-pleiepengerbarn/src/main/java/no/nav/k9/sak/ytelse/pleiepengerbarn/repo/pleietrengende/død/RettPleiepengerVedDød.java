package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.kodeverk.uttak.RettVedDødType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

import jakarta.persistence.*;

import java.time.LocalDateTime;
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

    @Column(name = "vurdert_av")
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt")
    private LocalDateTime vurdertTidspunkt;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public RettPleiepengerVedDød() {
        // Hibernate
    }

    public RettPleiepengerVedDød(String vurdering, RettVedDødType rettVedDødType, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this.vurdering = vurdering;
        this.rettVedDødType = rettVedDødType;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public Long getId() {
        return id;
    }

    public String getVurdering() {
        return vurdering;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
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
        return "RettVedPleietrengendeDød{" + "id=" + id + ", rettVedDødType=" + rettVedDødType + '}';
    }
}
