package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.medisinsk;

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

@Entity(name = "MedisinskGrunnlag")
@Table(name = "GR_MEDISINSK")
public class MedisinskGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MEDISINSK")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "legeerklaeringer_id", nullable = false, updatable = false, unique = true)
    private Legeerklæringer legeerklæringer;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "omsorgenfor_id", updatable = false, unique = true)
    private OmsorgenFor omsorgenFor;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "kontinuerlig_tilsyn_id", nullable = false, updatable = false, unique = true)
    private KontinuerligTilsyn kontinuerligTilsyn;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    MedisinskGrunnlag() {
    }

    MedisinskGrunnlag(Long behandlingId, KontinuerligTilsyn kontinuerligTilsyn, Legeerklæringer legeerklæringer,
                      OmsorgenFor omsorgenFor) {
        this.behandlingId = behandlingId;
        this.kontinuerligTilsyn = kontinuerligTilsyn; // NOSONAR
        this.legeerklæringer = legeerklæringer;
        this.omsorgenFor = omsorgenFor;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public KontinuerligTilsyn getKontinuerligTilsyn() {
        return kontinuerligTilsyn;
    }

    public Legeerklæringer getLegeerklæringer() {
        return legeerklæringer;
    }

    public OmsorgenFor getOmsorgenFor() {
        return omsorgenFor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedisinskGrunnlag)) return false;
        var that = (MedisinskGrunnlag) o;
        return Objects.equals(kontinuerligTilsyn, that.kontinuerligTilsyn) &&
            Objects.equals(omsorgenFor, that.omsorgenFor) &&
            Objects.equals(legeerklæringer, that.legeerklæringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kontinuerligTilsyn, omsorgenFor, legeerklæringer);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", kontinuerligTilsyn=" + kontinuerligTilsyn +
            ", legeerklæringer=" + legeerklæringer +
            ", omsorgenFor=" + omsorgenFor +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }
}
