package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "OmsorgenForGrunnlag")
@Table(name = "GR_OMSORGEN_FOR")
public class OmsorgenForGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MEDISINSK")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;


    @ManyToOne
    @Immutable
    @JoinColumn(name = "psb_omsorgen_for_id", nullable = false, updatable = false, unique = true)
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
        this.omsorgenFor = new OmsorgenFor(omsorgenFor);
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
