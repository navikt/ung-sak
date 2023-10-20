package no.nav.k9.sak.behandlingslager.notat;

import java.util.UUID;

import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
@MappedSuperclass
public abstract class NotatEntitet extends BaseEntitet {

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

}
