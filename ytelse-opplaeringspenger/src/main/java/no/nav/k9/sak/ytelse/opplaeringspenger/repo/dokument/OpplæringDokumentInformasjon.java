package no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument;

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
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentType;

//TODO fjern denne?
@Entity(name = "OpplæringDokumentInformasjon")
@Table(name = "OPPLAERING_DOKUMENT_INFORMASJON")
public class OpplæringDokumentInformasjon implements Comparable<OpplæringDokumentInformasjon> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OPPLAERING_DOKUMENT_INFORMASJON")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OPPLAERING_DOKUMENT_ID")
    private OpplæringDokument dokument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DUPLIKAT_AV_OPPLAERING_DOKUMENT_ID")
    private OpplæringDokument duplikatAvDokument; //TODO trengs ikke

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @Column(name = "TYPE", nullable = false)
    @Convert(converter = OpplæringDokumentTypeConverter.class)
    private OpplæringDokumentType type;

    @Column(name = "har_info_som_ikke_kan_punsjes")
    private boolean harInfoSomIkkeKanPunsjes; //TODO trengs ikke

    @Column(name = "DATERT")
    private LocalDate datert;

    @Column(name = "MOTTATT", nullable = false)
    private LocalDateTime mottatt;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable = false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable = false)
    private LocalDateTime opprettetTidspunkt;

    OpplæringDokumentInformasjon() {
    }

    public OpplæringDokumentInformasjon(
        OpplæringDokument dokument,
        OpplæringDokument duplikatAvDokument,
        OpplæringDokumentType type,
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

    public OpplæringDokumentInformasjon(
        OpplæringDokumentType type,
        boolean harInfoSomIkkeKanPunsjes,
        LocalDate datert,
        LocalDateTime mottatt,
        Long versjon,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this(null, null, type, harInfoSomIkkeKanPunsjes, datert, mottatt, versjon, opprettetAv, opprettetTidspunkt);
    }

    public Long getId() {
        return id;
    }

    public OpplæringDokumentType getType() {
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

    public void setDokument(OpplæringDokument dokument) {
        this.dokument = dokument;
    }

    public OpplæringDokument getDokument() {
        return dokument;
    }

    public OpplæringDokument getDuplikatAvDokument() {
        return duplikatAvDokument;
    }

    public void setDuplikatAvDokument(OpplæringDokument duplikatAvDokument) {
        this.duplikatAvDokument = duplikatAvDokument;
    }

    public Long getVersjon() {
        return versjon;
    }

    public void setType(OpplæringDokumentType type) {
        this.type = type;
    }

    @Override
    public int compareTo(OpplæringDokumentInformasjon o) {
        return Long.valueOf(versjon).compareTo(o.versjon);
    }
}
