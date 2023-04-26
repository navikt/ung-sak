package no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;
import no.nav.k9.sak.typer.JournalpostId;

@Entity(name = "OpplæringDokument")
@Table(name = "OPPLAERING_DOKUMENT")
public class OpplæringDokument extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPLAERING_DOKUMENT")
    @Column(unique = true, nullable = false)
    private Long id;

    @Column(name = "JOURNALPOST_ID", nullable = false)
    private JournalpostId journalpostId;

    @Column(name = "DOKUMENT_INFO_ID")
    private String dokumentInfoId;

    @Column(name = "TYPE", nullable = false)
    @Convert(converter = OpplæringDokumentTypeConverter.class)
    private OpplæringDokumentType type;

    @Column(name = "SOEKERS_BEHANDLING_UUID", nullable = false)
    private UUID søkersBehandlingUuid;

    @Column(name = "DATERT")
    private LocalDate datert;

    @Column(name = "MOTTATT", nullable = false)
    private LocalDateTime mottatt;

    OpplæringDokument() {
    }

    public OpplæringDokument(JournalpostId journalpostId, String dokumentInfoId, OpplæringDokumentType type, UUID søkersBehandlingUuid, LocalDate datert, LocalDateTime mottatt) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");;
        this.dokumentInfoId = dokumentInfoId;
        this.type = Objects.requireNonNull(type, "type");
        this.søkersBehandlingUuid = søkersBehandlingUuid;
        this.datert = datert;
        this.mottatt = Objects.requireNonNull(mottatt, "mottatt");
    }

    public OpplæringDokumentType getType() {
        return type;
    }

    public LocalDate getDatert() {
        return datert;
    }

    public LocalDateTime getMottatt() {
        return mottatt;
    }

    public UUID getSøkersBehandlingUuid() {
        return søkersBehandlingUuid;
    }

    public Long getId() {
        return id;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentInfoId() {
        return dokumentInfoId;
    }
}
