package no.nav.k9.sak.behandlingslager.saksnummer;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "SaksnummerAktørKobling")
@Table(name = "SAKSNUMMER_AKTOR")
public class SaksnummerAktørKoblingEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAKSNUMMER_AKTOR")
    @Column(name = "id")
    private Long id;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer", nullable = false, updatable = false)))
    private Saksnummer saksnummer;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "aktoer_id", nullable = false, updatable = false)))
    private AktørId aktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id", nullable = false, updatable=false)))
    private JournalpostId journalpostId;

    @Column(name = "slettet", nullable = false)
    private boolean slettet = false;

    SaksnummerAktørKoblingEntitet() {
        // for hibernate
    }

    public SaksnummerAktørKoblingEntitet(String saksnummer, String aktørId, String journalpostId) {
        this.saksnummer = new Saksnummer(saksnummer);
        this.aktørId = new AktørId(aktørId);
        this.journalpostId = new JournalpostId(journalpostId);
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public void setSlettet() {
        this.slettet = true;
    }
}
