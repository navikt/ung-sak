package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.klage.KlageVurdering;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

import java.util.Objects;

@Entity(name = "KlageVurderingEntitet")
@Table(name = "KLAGE_VURDERING")
public class KlageVurderingEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_VURDERING")
    private Long id;

    @Column(name = "klage_behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Convert(converter = KlageVurdertAvKodeverdiConverter.class)
    @Column(name = "klage_vurdert_av", nullable = false)
    private KlageVurdertAv vurdertAvEnhet;

    @Embedded
    private Vurderingresultat klageresultat = Vurderingresultat.TomtResultat;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "klage_formkrav_id")
    private KlageFormkravEntitet formkravEntitet;

    public KlageVurderingEntitet() {
        // Hibernate
    }

    public Long getId() {
        return id;
    }

    public KlageVurdertAv getVurdertAvEnhet() {
        return vurdertAvEnhet;
    }

    public KlageVurdering getKlageVurdering() {
        if (klageresultat != null) {
            return klageresultat.getKlageVurdering();
        }
        if (formkravEntitet.tilFormkrav().erAvvist()) {
            return KlageVurdering.AVVIS_KLAGE;
        }
        return null;
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

    public void slettFormkrav() {
        this.formkravEntitet = null;
    }

    public KlageFormkravAdapter getFormkrav() {
        // Formkrav er ikke tilgjengelig for klagevurdering mottatt fra Kabal
        return formkravEntitet == null ? null : formkravEntitet.tilFormkrav();
    }

    public void setFormkrav(KlageFormkravAdapter formkrav) {
        if (!harFormkrav()) {
            formkravEntitet = new KlageFormkravEntitet();
        }
        formkravEntitet.oppdater(formkrav);
    }

    public boolean harFormkrav() {
        return formkravEntitet != null;
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
            && Objects.equals(this.behandlingId, other.behandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vurdertAvEnhet, behandlingId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private KlageVurderingEntitet klageVurderingMal;

        public Builder() {
            klageVurderingMal = new KlageVurderingEntitet();
        }

        public Builder medKlageKlagebehandling(Behandling klageBehandling) {
            klageVurderingMal.behandlingId = klageBehandling.getId();
            return this;
        }

        public Builder medKlageVurdertAv(KlageVurdertAv klageVurdertAv) {
            klageVurderingMal.vurdertAvEnhet = klageVurdertAv;
            return this;
        }

        public Builder medFormkrav(KlageFormkravAdapter formkrav) {
            klageVurderingMal.formkravEntitet = KlageFormkravEntitet.builder()
                .medFormkrav(formkrav)
                .build();
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
            Objects.requireNonNull(klageVurderingMal.behandlingId, "klageBehandling");
            Objects.requireNonNull(klageVurderingMal.vurdertAvEnhet, "klageVurdertAv");
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
