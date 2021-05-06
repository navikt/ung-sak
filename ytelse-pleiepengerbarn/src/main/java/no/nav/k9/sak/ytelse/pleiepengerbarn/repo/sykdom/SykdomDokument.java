package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.JoinFormula;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "SykdomDokument")
@Table(name = "SYKDOM_DOKUMENT")
public class SykdomDokument {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DOKUMENT")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private SykdomVurderinger sykdomVurderinger;

    @ManyToOne
    @JoinFormula("(SELECT i.id FROM SYKDOM_DOKUMENT_INFORMASJON i where i.SYKDOM_DOKUMENT_ID = id ORDER BY i.VERSJON DESC LIMIT 1)")
    private SykdomDokumentInformasjon informasjon;

    @Column(name = "JOURNALPOST_ID", nullable = false)
    private JournalpostId journalpostId;

    @Column(name = "DOKUMENT_INFO_ID", nullable = true)
    private String dokumentInfoId;

    @Column(name = "BEHANDLING_UUID")
    private UUID behandlingUuid;

    @Column(name = "SAKSNUMMER")
    private Saksnummer saksnummer;

    @Column(name = "har_info_som_ikke_kan_punsjes")
    private boolean harInfoSomIkkeKanPunsjes;

    @ManyToOne
    @JoinColumn(name = "PERSON_ID")
    private SykdomPerson person;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomDokument() {

    }

    public SykdomDokument(
        JournalpostId journalpostId,
        String dokumentInfoId,
        SykdomDokumentInformasjon informasjon,
        UUID behandlingUuid,
        Saksnummer saksnummer,
        boolean harInfoSomIkkeKanPunsjes, SykdomPerson person,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.dokumentInfoId = dokumentInfoId;
        this.harInfoSomIkkeKanPunsjes = harInfoSomIkkeKanPunsjes;
        if (informasjon.getDokument() != null && informasjon.getDokument() != this) {
            throw new IllegalStateException("Potensiell krysskobling av dokumentInformasjon fra andre dokumenter!");
        }
        informasjon.setDokument(this);
        this.informasjon = informasjon;
        this.behandlingUuid = behandlingUuid;
        this.saksnummer = saksnummer;
        this.person = person;
        this.opprettetAv = Objects.requireNonNull(opprettetAv, "opprettetAv");
        this.opprettetTidspunkt = Objects.requireNonNull(opprettetTidspunkt, "opprettetTidspunkt");
    }

    public SykdomDokumentType getType() {
        if (informasjon == null) {
            throw new IllegalStateException("Dokument er ikke riktig initialisert!");
        }
        return informasjon.getType();
    }

    public LocalDate getDatert() {
        if (informasjon == null) {
            throw new IllegalStateException("Dokument er ikke riktig initialisert!");
        }
        return informasjon.getDatert();
    }

    public LocalDate getMottattDato() {
        if (informasjon == null) {
            throw new IllegalStateException("Dokument er ikke riktig initialisert!");
        }
        return informasjon.getMottattDato();
    }

    public LocalDateTime getMottattTidspunkt() {
        if (informasjon == null) {
            throw new IllegalStateException("Dokument er ikke riktig initialisert!");
        }
        return informasjon.getMottattTidspunkt();
    }

    public Long getVersjon() {
        if (informasjon == null) {
            throw new IllegalStateException("Dokument er ikke riktig initialisert!");
        }
        return informasjon.getVersjon();
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public SykdomPerson getPerson() {
        return person;
    }


    public Long getId() {
        return id;
    }

    void setSykdomVurderinger(SykdomVurderinger sykdomVurderinger) {
        this.sykdomVurderinger = sykdomVurderinger;
    }

    public SykdomVurderinger getSykdomVurderinger() {
        return sykdomVurderinger;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentInfoId() {
        return dokumentInfoId;
    }

    public void setInformasjon(SykdomDokumentInformasjon informasjon) {
        if (informasjon.getDokument() != null && informasjon.getDokument() != this) {
            throw new IllegalStateException("Potensiell krysskobling av dokumentInformasjon fra andre dokumenter!");
        }
        informasjon.setDokument(this);
        this.informasjon = informasjon;
    }

    public SykdomDokumentInformasjon getInformasjon() {
        return informasjon;
    }

    public boolean isHarInfoSomIkkeKanPunsjes() {
        return harInfoSomIkkeKanPunsjes;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public String getEndretAv() {
        return informasjon.getOpprettetAv();
    }

    public LocalDateTime getEndretTidspunkt() {
        return informasjon.getOpprettetTidspunkt();
    }
}
