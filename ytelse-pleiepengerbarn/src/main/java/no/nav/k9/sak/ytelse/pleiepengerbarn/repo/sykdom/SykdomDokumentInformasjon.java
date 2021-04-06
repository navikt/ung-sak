package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.JoinFormula;

import net.bytebuddy.asm.Advice;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.JournalpostId;

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
            LocalDate datert,
            LocalDateTime mottatt,
            Long versjon,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.type = Objects.requireNonNull(type, "type");
        this.datert = datert;
        this.mottatt = mottatt;
        this.versjon = versjon;
        this.opprettetAv = Objects.requireNonNull(opprettetAv, "opprettetAv");
        this.opprettetTidspunkt = Objects.requireNonNull(opprettetTidspunkt, "opprettetTidspunkt");
    }

    public SykdomDokumentInformasjon(
        SykdomDokument dokument,
        SykdomDokumentType type,
        LocalDate datert,
        LocalDateTime mottatt,
        Long versjon,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this(type, datert, mottatt, versjon, opprettetAv, opprettetTidspunkt);
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
