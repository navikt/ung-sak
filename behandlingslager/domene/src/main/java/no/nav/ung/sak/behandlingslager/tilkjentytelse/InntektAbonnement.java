package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.AktørId;

@Entity(name = "InntektAbonnement")
@Table(name = "INNTEKT_ABONNEMENT")
public class InntektAbonnement extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNTEKT_ABONNEMENT")
    private Long id;

    @Column(name = "abonnement_id", nullable = false, unique = true)
    private String abonnementId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false)))
    private AktørId aktørId;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    public InntektAbonnement() {
    }

    public InntektAbonnement(String abonnementId, AktørId aktørId) {
        this.abonnementId = abonnementId;
        this.aktørId = aktørId;
    }

    public Long getId() {
        return id;
    }

    public String getAbonnementId() {
        return abonnementId;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public boolean erAktiv() {
        return aktiv;
    }
    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }
    public long getVersjon() {
        return versjon;
    }

    public void setVersjon(long versjon) {
        this.versjon = versjon;
    }
}
