package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak;

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

@Entity(name = "EtablertTilsynGrunnlag")
@Table(name = "GR_ETABLERT_TILSYN")
public class EtablertTilsynGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_ETABLERT_TILSYN")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;


    @ManyToOne
    @Immutable
    @JoinColumn(name = "etablert_tilsyn_id", nullable = false, updatable = false, unique = true)
    private EtablertTilsyn etablertTilsyn;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    EtablertTilsynGrunnlag() {
    }

    EtablertTilsynGrunnlag(Long behandlingId, EtablertTilsyn etablertTilsyn) {
        this.behandlingId = behandlingId;
        this.etablertTilsyn = etablertTilsyn;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public EtablertTilsyn getEtablertTilsyn() {
        return etablertTilsyn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EtablertTilsynGrunnlag)) return false;
        var that = (EtablertTilsynGrunnlag) o;
        return Objects.equals(etablertTilsyn, that.etablertTilsyn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(etablertTilsyn);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", etablertTilsyn=" + etablertTilsyn +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }
}
