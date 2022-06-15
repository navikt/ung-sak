package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

//TODO: SykdomDiagnoser
@Entity(name = "SykdomDiagnosekoder")
@Table(name = "SYKDOM_DIAGNOSEKODER")
public class SykdomDiagnosekoder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DIAGNOSEKODER")
    private Long id;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @ManyToOne
    @JoinColumn(name = "SYKDOM_VURDERINGER_ID" )
    private SykdomVurderinger vurderinger;

    @OneToMany(mappedBy = "diagnosekoder", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private List<SykdomDiagnosekode> diagnosekoder;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    SykdomDiagnosekoder() {
        // hibernate
    }

    public SykdomDiagnosekoder(
        Long versjon,
        List<SykdomDiagnosekode> diagnosekoder,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this.versjon = versjon;
        this.diagnosekoder = diagnosekoder.stream()
            .map(k -> {
                if(k.getDiagnosekoder() != null && k.getDiagnosekoder() != this) {
                    throw new IllegalStateException("Potensiell krysskobling av koder fra andre diagnosekodesett!");
                }
                k.setDiagnosekoder(this);
                return k;
            }).collect(Collectors.toCollection(ArrayList::new));
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public SykdomDiagnosekoder(
            Long versjon,
            SykdomVurderinger vurderinger,
            List<SykdomDiagnosekode> diagnosekoder,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this(versjon, diagnosekoder, opprettetAv, opprettetTidspunkt);
        this.vurderinger = vurderinger;
    }

    public Long getId() {
        return id;
    }

    public Long getVersjon() {
        return versjon;
    }

    public void setVersjon(Long versjon) {
        this.versjon = versjon;
    }

    public SykdomVurderinger getVurderinger() {
        return vurderinger;
    }

    public void setVurderinger(SykdomVurderinger vurderinger) {
        this.vurderinger = vurderinger;
    }

    public void leggTilDiagnosekode(SykdomDiagnosekode kode) {
        if(kode.getDiagnosekoder() != null && kode.getDiagnosekoder() != this) {
            throw new IllegalStateException("Potensiell kryssko");
        }
        kode.setDiagnosekoder(this);
        diagnosekoder.add(kode);
    }

    public List<SykdomDiagnosekode> getDiagnosekoder() {
        return Collections.unmodifiableList(diagnosekoder);
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

}
