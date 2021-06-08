package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;

@Entity(name = "SykdomDokumentInformasjon")
@Table(name = "SYKDOM_DOKUMENT_INFORMASJON")
public class SykdomDokumentInformasjon {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DOKUMENT_INFORMASJON")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SYKDOM_DOKUMENT_ID")
    private SykdomDokument dokument;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @Column(name = "TYPE", nullable = false)
    @Convert(converter = SykdomDokumentTypeConverter.class)
    private SykdomDokumentType type;

    @Column(name = "har_info_som_ikke_kan_punsjes")
    private boolean harInfoSomIkkeKanPunsjes;

    @Column(name = "DATERT", nullable = true)
    private LocalDate datert;

    @Column(name = "MOTTATT", nullable = false)
    private LocalDateTime mottatt;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomDokumentInformasjon() {

    }

    public SykdomDokumentInformasjon(
            SykdomDokumentType type,
            boolean harInfoSomIkkeKanPunsjes,
            LocalDate datert,
            LocalDateTime mottatt,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.type = Objects.requireNonNull(type, "type");
        this.datert = datert;
        this.mottatt = mottatt;
        this.versjon = versjon;
        this.harInfoSomIkkeKanPunsjes = harInfoSomIkkeKanPunsjes;
        this.opprettetAv = Objects.requireNonNull(opprettetAv, "opprettetAv");
        this.opprettetTidspunkt = Objects.requireNonNull(opprettetTidspunkt, "opprettetTidspunkt");
    }

    public SykdomDokumentInformasjon(
            SykdomDokument dokument,
            SykdomDokumentType type,
            boolean harInfoSomIkkeKanPunsjes,
            LocalDate datert,
            LocalDateTime mottatt,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt
            ) {
        this(type, harInfoSomIkkeKanPunsjes, datert, mottatt, versjon,  opprettetAv, opprettetTidspunkt);
        this.dokument = dokument;
    }

    public Long getId() {
        return id;
    }

    public SykdomDokumentType getType() {
        return type;
    }

    public LocalDateTime getMottattTidspunkt() {
        return mottatt;
    }

    public LocalDate getMottattDato() {
        return mottatt.toLocalDate();
    }

    public LocalDate getDatert() {
        return datert;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public boolean isHarInfoSomIkkeKanPunsjes() {
        return harInfoSomIkkeKanPunsjes;
    }

    public void setDokument(SykdomDokument dokument) {
        this.dokument = dokument;
    }

    public SykdomDokument getDokument() {
        return dokument;
    }

    public Long getVersjon() {
        return versjon;
    }

    public void setType(SykdomDokumentType type) {
        this.type = type;
    }
}
