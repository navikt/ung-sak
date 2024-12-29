package no.nav.ung.sak.formidling.domene;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "BrevbestillingEntitet")
@Table(name = "brevbestilling")
public class BrevbestillingEntitet extends BaseEntitet {
    @Id
    @SequenceGenerator(name = "seq_brevbestilling", sequenceName = "seq_brevbestilling")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_brevbestilling")
    private Long id;

    @Column(name = "brevbestilling_uuid", updatable = false, nullable = false)
    private UUID brevbestillingUuid;

    @Column(name = "saksnummer", updatable = false, nullable = false)
    private String saksnummer;

    @Column(name = "dokumentmal_type", updatable = false, nullable = false)
    @Convert(converter = DokumentMalTypeKodeverdiConverter.class)
    private DokumentMalType dokumentMalType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BrevbestillingStatusType status;

    @Column(name = "dokumentdata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) //TODO endre til å bruke json subtypes?
    private String dokumentdata;

    @Column(name = "journalpost_id")
    private String journalpostId;

    @Column(name = "dokdist_bestilling_id")
    private String dokdistBestillingId;

    @Embedded
    private BrevMottaker mottaker;


    public BrevbestillingEntitet(String saksnummer, DokumentMalType dokumentMalType, BrevbestillingStatusType status, String dokumentdata, BrevMottaker mottaker) {
        this.brevbestillingUuid = UUID.randomUUID();
        this.saksnummer = saksnummer;
        this.dokumentMalType = dokumentMalType;
        this.status = status;
        this.dokumentdata = dokumentdata;
        this.mottaker = mottaker;
    }

    public BrevbestillingEntitet() {
    }

    public UUID getBrevbestillingUuid() {
        return brevbestillingUuid;
    }

    public BrevbestillingStatusType getStatus() {
        return status;
    }

    public String getJournalpostId() {
        return null;
    }

    public String getDistribusjonsId() {
        return null;
    }

    public String getSaksnummer() {
        return saksnummer;
    }
}
