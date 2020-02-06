package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@Entity(name = "MedisinskGrunnlag")
@Table(name = "GR_MEDISINSK")
public class MedisinskGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_MEDISINSK")
    private Long id;

    @OneToOne
    @JoinColumn(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Behandling behandling;

    @OneToOne
    @Immutable
    @JoinColumn(name = "legeerklaeringer_id", nullable = false, updatable = false, unique = true)
    private Legeerklæringer legeerklæringer;

    @OneToOne
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

    MedisinskGrunnlag(Behandling behandling, KontinuerligTilsyn kontinuerligTilsyn, Legeerklæringer legeerklæringer) {
        this.behandling = behandling;
        this.kontinuerligTilsyn = kontinuerligTilsyn; // NOSONAR
        this.legeerklæringer = legeerklæringer;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public KontinuerligTilsyn getKontinuerligTilsyn() {
        return kontinuerligTilsyn;
    }

    void setKontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        this.kontinuerligTilsyn = kontinuerligTilsyn;
    }

    public Legeerklæringer getLegeerklæringer() {
        return legeerklæringer;
    }

    void setLegeerklæringer(Legeerklæringer legeerklæringer) {
        this.legeerklæringer = legeerklæringer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedisinskGrunnlag that = (MedisinskGrunnlag) o;
        return Objects.equals(kontinuerligTilsyn, that.kontinuerligTilsyn) &&
            Objects.equals(legeerklæringer, that.legeerklæringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kontinuerligTilsyn, legeerklæringer);
    }

    @Override
    public String toString() {
        return "MedisinskGrunnlag{" +
            "id=" + id +
            ", behandling=" + behandling +
            ", kontinuerligTilsyn=" + kontinuerligTilsyn +
            ", legeerklæringer=" + legeerklæringer +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '}';
    }
}
