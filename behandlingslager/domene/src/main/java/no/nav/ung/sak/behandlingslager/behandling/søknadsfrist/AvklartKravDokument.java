package no.nav.ung.sak.behandlingslager.behandling.søknadsfrist;

import java.time.LocalDate;
import java.util.Objects;

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

import org.hibernate.annotations.Immutable;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.typer.JournalpostId;

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

    @Column(name = "godkjent", nullable = false)
    private boolean godkjent;

    @Column(name = "gyldig_fra", nullable = false)
    private LocalDate fraDato;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    AvklartKravDokument() {
        // hibernate
    }

    public AvklartKravDokument(JournalpostId journalpostId, Boolean godkjent, LocalDate fraDato, String begrunnelse) {
        this.journalpostId = journalpostId;
        this.godkjent = godkjent;
        this.fraDato = fraDato;
        this.begrunnelse = begrunnelse;
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

    public LocalDate getFraDato() {
        return fraDato;
    }

    public String getBegrunnelse() {
        return begrunnelse;
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
