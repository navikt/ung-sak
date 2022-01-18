package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov;

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
    private EtablertPleieperioder pleieperioder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    PleiebehovResultat() {
    }

    PleiebehovResultat(Long behandlingId, EtablertPleieperioder pleieperioder) {
        this.behandlingId = behandlingId;
        this.pleieperioder = pleieperioder; // NOSONAR
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public EtablertPleieperioder getPleieperioder() {
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
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", kontinuerligTilsyn=" + pleieperioder +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }
}
