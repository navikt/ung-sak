package no.nav.k9.sak.behandlingslager.behandling.søknadsfrist;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "AvklartSøknadsfristDokument")
@Table(name = "SF_AVKLART_DOKUMENT")
@Immutable
public class AvklartKravDokument extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SF_AVKLART_DOKUMENT")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "journalpostId", column = @Column(name = "journalpost_id")))
    private JournalpostId journalpostId;

    @Column(name = "godkjent")
    private boolean godkjent;

    @Column(name = "godkjent_fra_dato")
    private LocalDate godkjentFraDato;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    AvklartKravDokument() {
        // hibernate
    }

    AvklartKravDokument(AvklartKravDokument periode) {
        this.journalpostId = periode.getJournalpostId();
    }

    public AvklartKravDokument(JournalpostId journalpostId, Boolean godkjent, LocalDate godkjentFraDato) {
        this.journalpostId = journalpostId;
        this.godkjent = godkjent;
        this.godkjentFraDato = godkjentFraDato;
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public boolean getErGodkjent() {
        return godkjent;
    }

    public Utfall getUtfall() {
        if (godkjent) {
            return Utfall.OPPFYLT;
        } else {
            return Utfall.IKKE_OPPFYLT;
        }
    }

    public LocalDate getGodkjentFraDato() {
        return godkjentFraDato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AvklartKravDokument that = (AvklartKravDokument) o;
        return Objects.equals(journalpostId, that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return "AvklartSøknadsfristDokument{" +
            "journalpostId=" + journalpostId +
            '}';
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(journalpostId);
    }
}
