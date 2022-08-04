package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;

@Entity(name = "PleietrengendeSykdomDokumentInformasjon")
@Table(name = "PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON")
public class PleietrengendeSykdomDokumentInformasjon implements Comparable<PleietrengendeSykdomDokumentInformasjon> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_DOKUMENT_ID")
    private PleietrengendeSykdomDokument dokument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DUPLIKAT_AV_PLEIETRENGENDE_SYKDOM_DOKUMENT_ID")
    private PleietrengendeSykdomDokument duplikatAvDokument;

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

    PleietrengendeSykdomDokumentInformasjon() {

    }

    public PleietrengendeSykdomDokumentInformasjon(
            PleietrengendeSykdomDokument dokument,
            PleietrengendeSykdomDokument duplikatAvDokument,
            SykdomDokumentType type,
            boolean harInfoSomIkkeKanPunsjes,
            LocalDate datert,
            LocalDateTime mottatt,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt
            ) {
        this.type = Objects.requireNonNull(type, "type");
        this.datert = datert;
        this.mottatt = mottatt;
        this.versjon = versjon;
        this.harInfoSomIkkeKanPunsjes = harInfoSomIkkeKanPunsjes;
        this.opprettetAv = Objects.requireNonNull(opprettetAv, "opprettetAv");
        this.opprettetTidspunkt = Objects.requireNonNull(opprettetTidspunkt, "opprettetTidspunkt");
        this.dokument = dokument;
        this.duplikatAvDokument = duplikatAvDokument;
    }

    public PleietrengendeSykdomDokumentInformasjon(
            SykdomDokumentType type,
            boolean harInfoSomIkkeKanPunsjes,
            LocalDate datert,
            LocalDateTime mottatt,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this(null, null, type, harInfoSomIkkeKanPunsjes, datert, mottatt, versjon,  opprettetAv, opprettetTidspunkt);
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

    public void setDokument(PleietrengendeSykdomDokument dokument) {
        this.dokument = dokument;
    }

    public PleietrengendeSykdomDokument getDokument() {
        return dokument;
    }

    public PleietrengendeSykdomDokument getDuplikatAvDokument() {
        return duplikatAvDokument;
    }

    public void setDuplikatAvDokument(PleietrengendeSykdomDokument duplikatAvDokument) {
        this.duplikatAvDokument = duplikatAvDokument;
    }

    public Long getVersjon() {
        return versjon;
    }

    public void setType(SykdomDokumentType type) {
        this.type = type;
    }

    @Override
    public int compareTo(PleietrengendeSykdomDokumentInformasjon o) {
        return Long.valueOf(versjon).compareTo(o.versjon);
    }
}
