package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "PleietrengendeSykdomDokument")
@Table(name = "PLEIETRENGENDE_SYKDOM_DOKUMENT")
public class PleietrengendeSykdomDokument {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM_DOKUMENT")
    @Column(unique = true, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private PleietrengendeSykdom pleietrengendeSykdom;

    @OneToMany(mappedBy = "dokument", cascade = CascadeType.PERSIST)
    private List<PleietrengendeSykdomDokumentInformasjon> informasjoner = new ArrayList<>();

    @Column(name = "JOURNALPOST_ID", nullable = false)
    private JournalpostId journalpostId;

    @Column(name = "DOKUMENT_INFO_ID", nullable = true)
    private String dokumentInfoId;

    @Column(name = "SOEKERS_BEHANDLING_UUID")
    private UUID søkersBehandlingUuid;

    @Column(name = "SOEKERS_SAKSNUMMER")
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "soekers_saksnummer")))
    private Saksnummer søkersSaksnummer;

    @ManyToOne
    @JoinColumn(name = "SOEKERS_PERSON_ID")
    private Person søker;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomDokument() {

    }

    public PleietrengendeSykdomDokument(
            JournalpostId journalpostId,
            String dokumentInfoId,
            PleietrengendeSykdomDokumentInformasjon informasjon,
            UUID søkersBehandlingUuid,
            Saksnummer søkersSaksnummer,
            Person søker,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.dokumentInfoId = dokumentInfoId;
        setInformasjon(informasjon);
        this.søkersBehandlingUuid = søkersBehandlingUuid;
        this.søkersSaksnummer = søkersSaksnummer;
        this.søker = søker;
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
    public PleietrengendeSykdomDokument getDuplikatAvDokument() {
        return getInformasjon().getDuplikatAvDokument();
    }

    public boolean isDuplikat() {
        return getInformasjon().getDuplikatAvDokument() != null;
    }

    public UUID getSøkersBehandlingUuid() {
        return søkersBehandlingUuid;
    }

    public Saksnummer getSøkersSaksnummer() {
        return søkersSaksnummer;
    }

    public Person getSøker() {
        return søker;
    }

    public Long getId() {
        return id;
    }

    void setSykdomVurderinger(PleietrengendeSykdom pleietrengendeSykdom) {
        this.pleietrengendeSykdom = pleietrengendeSykdom;
    }

    public PleietrengendeSykdom getSykdomVurderinger() {
        return pleietrengendeSykdom;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentInfoId() {
        return dokumentInfoId;
    }

    public void setInformasjon(PleietrengendeSykdomDokumentInformasjon informasjon) {
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

    public PleietrengendeSykdomDokumentInformasjon getInformasjon() {
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
