package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.Objects;

@Entity(name = "KlageVurderingEntitet")
@Table(name = "KLAGE_VURDERING")
public class KlageVurderingEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_VURDERING")
    private Long id;

    @Column(name = "klage_utredning_id", nullable = false, updatable = false, unique = true)
    private Long klageutredning_id;

    @Convert(converter = KlageVurdertAvKodeverdiConverter.class)
    @Column(name = "klage_vurdert_av", nullable = false)
    private KlageVurdertAv vurdertAvEnhet;

    @Embedded
    private Vurderingresultat klageresultat = Vurderingresultat.TomtResultat;

    @Column(name = "kabal_referanse", nullable = false, updatable = false, unique = true)
    private String kabalReferanse;

    public KlageVurderingEntitet() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public KlageVurdertAv getVurdertAvEnhet() {
        return vurdertAvEnhet;
    }

    public void fjernResultat() {
        klageresultat = Vurderingresultat.TomtResultat;
    }

    public boolean harKlageresultat() {
        return !klageresultat.equals(Vurderingresultat.TomtResultat);
    }

    public Vurderingresultat getKlageresultat() {
        return klageresultat;
    }

    public void setKlageresultat(Vurderingresultat klageresultat) {
        this.klageresultat = klageresultat;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageVurderingEntitet)) {
            return false;
        }
        KlageVurderingEntitet other = (KlageVurderingEntitet) obj;
        return Objects.equals(this.vurdertAvEnhet, other.vurdertAvEnhet)
            && Objects.equals(this.klageutredning_id, other.klageutredning_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurdertAvEnhet, klageutredning_id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KlageVurderingEntitet klageVurderingMal;

        public Builder() {
            klageVurderingMal = new KlageVurderingEntitet();
        }

        public Builder medKlageVurdertAv(KlageVurdertAv klageVurdertAv) {
            klageVurderingMal.vurdertAvEnhet = klageVurdertAv;
            return this;
        }

        public Builder medKlageutredningId(Long klageutredningId) {
            klageVurderingMal.klageutredning_id = klageutredningId;
            return this;
        }

        public Builder medResultat(Vurderingresultat resultat) {
            klageVurderingMal.klageresultat = resultat;
            return this;
        }

        public KlageVurderingEntitet build() {
            verifyStateForBuild();
            return klageVurderingMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageVurderingMal.klageutredning_id, "klageutredning_id er null");
            Objects.requireNonNull(klageVurderingMal.vurdertAvEnhet, "klageVurdertAvEnhet er null");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "klageVurdertAv=" + vurdertAvEnhet + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "klageResultat=" + klageresultat //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }
}
