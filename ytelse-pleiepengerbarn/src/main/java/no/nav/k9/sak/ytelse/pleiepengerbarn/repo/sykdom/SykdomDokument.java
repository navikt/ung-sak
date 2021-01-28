package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType.SykdomDokumentTypeConverter;

@Entity(name = "SykdomDokument")
@Table(name = "SYKDOM_DOKUMENT")
public class SykdomDokument {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DOKUMENT")
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID", nullable = false, updatable = false, unique = true) //TODO:modifiers
    private SykdomVurderinger sykdomVurderinger;

    @Column(name = "TYPE", nullable = false)
    @Convert(converter = SykdomDokumentTypeConverter.class)
    private SykdomDokumentType type;
    
    @Column(name = "DATERT", nullable = true)
    private LocalDate datert;
    
    @Column(name = "JOURNALPOST_ID", nullable = false)
    private JournalpostId journalpostId;

    @Column(name = "DOKUMENT_INFO_ID", nullable = true)
    private String dokumentInfoId;

    @OneToMany
    @JoinColumn(name = "SYKDOM_DOKUMENT_ID")
    private List<SykdomDokumentSak> dokumentSaker = new ArrayList<>();

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    @DiffIgnore
    @Column(name = "ENDRET_AV", nullable = false, updatable=false)
    private String endretAv;

    @DiffIgnore
    @Column(name = "ENDRET_TID", nullable = false, updatable=false)
    private LocalDateTime endretTidspunkt; // NOSONAR
    
    
    SykdomDokument() {
        
    }
    
    public SykdomDokument(SykdomDokumentType type, JournalpostId journalpostId, String dokumentInfoId,
            String opprettetAv, LocalDateTime opprettetTidspunkt, String endretAv, LocalDateTime endretTidspunkt) {
        this.type = Objects.requireNonNull(type, "type");
        this.journalpostId = Objects.requireNonNull(journalpostId, "journalpostId");
        this.dokumentInfoId = dokumentInfoId;
        this.opprettetAv = Objects.requireNonNull(opprettetAv, "opprettetAv");
        this.opprettetTidspunkt = Objects.requireNonNull(opprettetTidspunkt, "opprettetTidspunkt");
        this.endretAv = Objects.requireNonNull(endretAv, "endretAv");
        this.endretTidspunkt = Objects.requireNonNull(endretTidspunkt, "endretTidspunkt");
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
    
    public SykdomDokumentType getType() {
        return type;
    }
    
    public LocalDate getDatert() {
        return datert;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public String getDokumentInfoId() {
        return dokumentInfoId;
    }

    public List<SykdomDokumentSak> getDokumentSaker() {
        return dokumentSaker;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public String getEndretAv() {
        return endretAv;
    }

    public LocalDateTime getEndretTidspunkt() {
        return endretTidspunkt;
    }
    
    public void setType(SykdomDokumentType type) {
        this.type = type;
    }
    
    public void setDatert(LocalDate datert) {
        this.datert = datert;
    }
}
