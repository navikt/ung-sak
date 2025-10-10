package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.kodeverk.klage.KlageMedholdÅrsak;
import no.nav.ung.kodeverk.klage.KlageVurderingType;
import no.nav.ung.kodeverk.klage.KlageVurderingOmgjør;

import java.util.Objects;
import java.util.Optional;

@Embeddable
public class Vurderingresultat {

    public static final Vurderingresultat TomtResultat = new Vurderingresultat();

    @Convert(converter = KlageVurderingKodeverdiConverter.class)
    @Column(name = "klagevurdering", nullable = false)
    private KlageVurderingType klageVurdering = KlageVurderingType.UDEFINERT;

    @Convert(converter = KlageMedholdÅrsakKodeverdiConverter.class)
    @Column(name = "klage_omgjoer_aarsak", nullable = false)
    private KlageMedholdÅrsak klageOmgjørÅrsak = KlageMedholdÅrsak.UDEFINERT;

    @Convert(converter = KlageVurderingOmgjørKodeverdiConverter.class)
    @Column(name = "klage_vurdering_omgjoer", nullable = false)
    private KlageVurderingOmgjør klageVurderingOmgjør = KlageVurderingOmgjør.UDEFINERT;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Convert(converter = KlageVurderingHjemmelConverter.class)
    @Column(name = "hjemmel")
    private Hjemmel hjemmel = Hjemmel.MANGLER;

    public Vurderingresultat() {}

    public Vurderingresultat(KlageVurderingAdapter resultat) {
        Objects.requireNonNull(resultat.getKlageVurdering(), "klageVurdering");
        klageVurdering = resultat.getKlageVurdering();
        begrunnelse = resultat.getBegrunnelse();
        klageVurderingOmgjør = Optional.ofNullable(resultat.getKlageVurderingOmgjoer()).orElse(KlageVurderingOmgjør.UDEFINERT);
        klageOmgjørÅrsak = Optional.ofNullable(resultat.getKlageMedholdArsakKode()).orElse(KlageMedholdÅrsak.UDEFINERT);
        hjemmel = resultat.getHjemmel();
        verifyState();
    }

    public void verifyState() {
        if (klageVurdering.equals(KlageVurderingType.MEDHOLD_I_KLAGE)) {
            Objects.requireNonNull(klageOmgjørÅrsak, "klageOmgjørÅrsak");
            Objects.requireNonNull(klageVurderingOmgjør, "klageVurderingOmgjør");
        }
    }

    public Optional<KlageVurderingType> getKlageVurdering() {
        return Optional.ofNullable((klageVurdering == KlageVurderingType.UDEFINERT) ? null : klageVurdering);
    }

    public Optional<KlageMedholdÅrsak> getKlageOmgjørÅrsak() {
        return Optional.ofNullable((klageOmgjørÅrsak == KlageMedholdÅrsak.UDEFINERT) ? null : klageOmgjørÅrsak);
    }

    public Optional<KlageVurderingOmgjør> getKlageVurderingOmgjør() {
        return Optional.ofNullable((klageVurderingOmgjør == KlageVurderingOmgjør.UDEFINERT) ? null : klageVurderingOmgjør);
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageVurderingEntitet)) {
            return false;
        }
        Vurderingresultat other = (Vurderingresultat) obj;
        return Objects.equals(this.klageVurdering, other.klageVurdering)
            && Objects.equals(this.klageOmgjørÅrsak, other.klageOmgjørÅrsak)
            && Objects.equals(this.klageVurderingOmgjør, other.klageVurderingOmgjør);
    }

    @Override
    public int hashCode() {
        return Objects.hash(klageVurdering, klageOmgjørÅrsak, klageVurderingOmgjør);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
            + "klageVurdering=" + klageVurdering + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "klageVurderingOmgjør=" + klageVurderingOmgjør + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "klageOmgjørÅrsak=" + klageOmgjørÅrsak + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "begrunnelse=" + (begrunnelse != null ? "<" + begrunnelse.length() + ">" : "null") + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + "hjemmel=" + hjemmel + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }
}
