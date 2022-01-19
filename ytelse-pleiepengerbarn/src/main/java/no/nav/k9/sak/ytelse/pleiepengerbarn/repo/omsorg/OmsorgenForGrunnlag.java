package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "OmsorgenForGrunnlag")
@Table(name = "GR_OMSORGEN_FOR")
public class OmsorgenForGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_OMSORGEN_FOR")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;


    @ManyToOne
    @Immutable
    @JoinColumn(name = "omsorgen_for_id", nullable = false, updatable = false, unique = true)
    private OmsorgenFor omsorgenFor;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OmsorgenForGrunnlag() {
    }

    OmsorgenForGrunnlag(Long behandlingId, OmsorgenFor omsorgenFor) {
        this.behandlingId = behandlingId;
        this.omsorgenFor = omsorgenFor;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public OmsorgenFor getOmsorgenFor() {
        return omsorgenFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OmsorgenForGrunnlag)) return false;
        var that = (OmsorgenForGrunnlag) o;
        return Objects.equals(omsorgenFor, that.omsorgenFor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(omsorgenFor);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", omsorgenFor=" + omsorgenFor +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }
}
