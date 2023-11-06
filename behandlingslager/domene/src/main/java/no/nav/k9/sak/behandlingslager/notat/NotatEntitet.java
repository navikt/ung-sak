package no.nav.k9.sak.behandlingslager.notat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

/**
 * Reprensenterer notat.
 *
 */
@MappedSuperclass
public abstract class NotatEntitet extends BaseEntitet {

    /**
     * for å kunne skille på notater på tvers av rader i sak og aktør
     */
    @NaturalId
    @Column(name = "uuid", nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "skjult", nullable = false, updatable = true)
    private boolean skjult;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NotatEntitet() {}

    public NotatEntitet(boolean skjult) {
        this.skjult = skjult;
        this.uuid = UUID.randomUUID();
    }

    public abstract String getNotatTekst();

    public abstract void nyTekst(String tekst);

    public boolean isAktiv() {
        return aktiv;
    }

    public long getVersjon() {
        return versjon;
    }

    public boolean isSkjult() {
        return skjult;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void skjul(boolean skjul) {
        this.skjult = skjul;
    }

    public boolean kanRedigere(String userId) {
        return userId.equals(opprettetAv);
    }

    /**
     * Kun for test
     */
    void overstyrOpprettetTidspunkt(LocalDateTime tidspunkt) {
        setOpprettetTidspunkt(tidspunkt);
    }

}
