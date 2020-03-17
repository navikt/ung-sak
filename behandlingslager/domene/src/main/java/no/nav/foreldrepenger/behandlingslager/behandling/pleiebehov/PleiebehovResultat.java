package no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov;

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

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "PleiebehovResultat")
@Table(name = "RS_PLEIEBEHOV")
public class PleiebehovResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_PLEIEBEHOV")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "pleieperioder_id", nullable = false, updatable = false, unique = true)
    private Pleieperioder pleieperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    PleiebehovResultat() {
    }

    PleiebehovResultat(Long behandlingId, Pleieperioder pleieperioder) {
        this.behandlingId = behandlingId;
        this.pleieperioder = pleieperioder; // NOSONAR
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Pleieperioder getPleieperioder() {
        return pleieperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PleiebehovResultat that = (PleiebehovResultat) o;
        return Objects.equals(behandlingId, that.behandlingId)
            && Objects.equals(pleieperioder, that.pleieperioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, pleieperioder);
    }

    @Override
    public String toString() {
        return "PleiebehovResultat{" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", kontinuerligTilsyn=" + pleieperioder +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '}';
    }
}
