package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "SykdomDokument")
@Table(name = "SYKDOM_DOKUMENT")
public class SykdomDokument {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DOKUMENT")
    @Column(unique = true, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private SykdomVurderinger sykdomVurderinger;

    @OneToMany(mappedBy = "dokument", cascade = CascadeType.PERSIST)
    private List<SykdomDokumentInformasjon> informasjoner = new ArrayList<>();

    @Column(name = "JOURNALPOST_ID", nullable = false)
    private JournalpostId journalpostId;

    @Column(name = "DOKUMENT_INFO_ID", nullable = true)
    private String dokumentInfoId;

    @Column(name = "BEHANDLING_UUID")
    private UUID behandlingUuid;

    @Column(name = "SAKSNUMMER")
    private Saksnummer saksnummer;

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
            SykdomPerson person,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.dokumentInfoId = dokumentInfoId;
        setInformasjon(informasjon);
        this.behandlingUuid = behandlingUuid;
        this.saksnummer = saksnummer;
        this.person = person;
        this.opprettetAv = Objects.requireNonNull(opprettetAv, "opprettetAv");
        this.opprettetTidspunkt = Objects.requireNonNull(opprettetTidspunkt, "opprettetTidspunkt");
    }

    public SykdomDokumentType getType() {
        return getInformasjon().getType();
    }

    public LocalDate getDatert() {
        return getInformasjon().getDatert();
    }

    public LocalDate getMottattDato() {
        return getInformasjon().getMottattDato();
    }

    public LocalDateTime getMottattTidspunkt() {
        return getInformasjon().getMottattTidspunkt();
    }

    public Long getVersjon() {
        return getInformasjon().getVersjon();
    }

    /**
     * Henter dokumentet dette dokumentet er et duplikat av.
     * @return Dokumentet som skal brukes fremfor dette dokumentet, eller {@code null} hvis dette
     *  dokumentet ikke er et duplikat.
     */
    public SykdomDokument getDuplikatAvDokument() {
        return getInformasjon().getDuplikatAvDokument();
    }
    
    public boolean isDuplikat() {
        return getInformasjon().getDuplikatAvDokument() != null;
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
        Objects.requireNonNull(informasjon, "'informasjon' kan ikke være null.");

        if (informasjon.getId() != null) {
            throw new IllegalStateException("'informasjon' har allerede blitt lagret og kan derfor ikke settes på dette dokumentet.");
        }
        if (informasjon.getDokument() != null && informasjon.getDokument() != this) {
            throw new IllegalStateException("Potensiell krysskobling av dokumentInformasjon fra andre dokumenter!");
        }
        informasjon.setDokument(this);
        informasjoner.add(informasjon);
    }

    public SykdomDokumentInformasjon getInformasjon() {
        return informasjoner.stream().max(Comparator.naturalOrder()).orElse(null);
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public String getEndretAv() {
        return getInformasjon().getOpprettetAv();
    }

    public LocalDateTime getEndretTidspunkt() {
        return getInformasjon().getOpprettetTidspunkt();
    }

    public boolean isHarInfoSomIkkeKanPunsjes() {
        return getInformasjon().isHarInfoSomIkkeKanPunsjes();
    }
}
