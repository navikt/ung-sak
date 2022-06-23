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

@Entity(name = "PleietrengendeSykdomDiagnoser")
@Table(name = "PLEIETRENGENDE_SYKDOM_DIAGNOSER")
public class PleietrengendeSykdomDiagnoser {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_DIAGNOSER")
    private Long id;

    @Column(name = "VERSJON", nullable = false)
    private Long versjon;

    @ManyToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_ID" )
    private PleietrengendeSykdom pleietrengendeSykdom;

    @OneToMany(mappedBy = "diagnoser", cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    private List<PleietrengendeSykdomDiagnose> diagnoser;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR

    PleietrengendeSykdomDiagnoser() {
        // hibernate
    }

    public PleietrengendeSykdomDiagnoser(
        Long versjon,
        List<PleietrengendeSykdomDiagnose> diagnoser,
        String opprettetAv,
        LocalDateTime opprettetTidspunkt) {
        this.versjon = versjon;
        this.diagnoser = diagnoser.stream()
            .map(k -> {
                if(k.getDiagnoser() != null && k.getDiagnoser() != this) {
                    throw new IllegalStateException("Potensiell krysskobling av koder fra andre diagnosekodesett!");
                }
                k.setDiagnoser(this);
                return k;
            }).collect(Collectors.toCollection(ArrayList::new));
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public PleietrengendeSykdomDiagnoser(
            Long versjon,
            PleietrengendeSykdom pleietrengendeSykdom,
            List<PleietrengendeSykdomDiagnose> diagnoser,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this(versjon, diagnoser, opprettetAv, opprettetTidspunkt);
        this.pleietrengendeSykdom = pleietrengendeSykdom;
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

    public PleietrengendeSykdom getPleietrengendeSykdom() {
        return pleietrengendeSykdom;
    }

    public void setPleietrengendeSykdom(PleietrengendeSykdom vurderinger) {
        this.pleietrengendeSykdom = vurderinger;
    }

    public void leggTilDiagnosekode(PleietrengendeSykdomDiagnose kode) {
        if(kode.getDiagnoser() != null && kode.getDiagnoser() != this) {
            throw new IllegalStateException("Potensiell kryssko");
        }
        kode.setDiagnoser(this);
        diagnoser.add(kode);
    }

    public List<PleietrengendeSykdomDiagnose> getDiagnoser() {
        return Collections.unmodifiableList(diagnoser);
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

}
