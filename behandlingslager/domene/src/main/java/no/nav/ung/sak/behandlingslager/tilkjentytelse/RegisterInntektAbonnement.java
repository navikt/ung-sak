package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.typer.AktørId;

@Entity(name = "RegisterInntektAbonnement")
@Table(name = "REGISTER_INNTEKT_ABONNEMENT")
public class RegisterInntektAbonnement extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REGISTER_INNTEKT_ABONNEMENT")
    private Long id;

    @Column(name = "abonnement_id", nullable = false, unique = true)
    private String abonnementId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false)))
    private AktørId aktørId;

    public RegisterInntektAbonnement() {
    }

    public RegisterInntektAbonnement(String abonnementId, AktørId aktørId) {
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
}
