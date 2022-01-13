package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.*;
import java.util.Objects;

@Entity(name = "RettVedPleietrengendeDødGrunnlag")
@Table(name = "psb_gr_rett_pleiepenger_ved_doed")
public class RettPleiepengerVedDødGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_psb_gr_rett_pleiepenger_ved_doed")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "psb_rett_pleiepenger_ved_doed_id", nullable = false, updatable = false, unique = true)
    private RettPleiepengerVedDød rettPleiepengerVedDød;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    RettPleiepengerVedDødGrunnlag() {
    }

    RettPleiepengerVedDødGrunnlag(Long behandlingId, RettPleiepengerVedDød rettPleiepengerVedDød) {
        this.behandlingId = behandlingId;
        this.rettPleiepengerVedDød = rettPleiepengerVedDød;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public RettPleiepengerVedDød getRettVedPleietrengendeDød() {
        return rettPleiepengerVedDød;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RettPleiepengerVedDødGrunnlag that = (RettPleiepengerVedDødGrunnlag) o;
        return rettPleiepengerVedDød.equals(that.rettPleiepengerVedDød);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rettPleiepengerVedDød);
    }

    @Override
    public String toString() {
        return "RettVedPleietrengendeDødGrunnlag{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", rettVedPleietrengendeDød=" + rettPleiepengerVedDød +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '}';
    }
}
