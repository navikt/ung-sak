package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

//TODO: Sykdom_Behandling_Anvendte_Data?
@Entity(name = "SykdomGrunnlag")
@Table(name = "SYKDOM_GRUNNLAG")
public class SykdomGrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SYKDOM_GRUNNLAG")
    private Long id;

    @Column(name = "SYKDOM_GRUNNLAG_UUID", nullable = false)
    private UUID sykdomGrunnlagUUID;

    //TODO: Deprekere, hente data rett fra gr_soeknadsperiode i stedet.
    @OneToMany(mappedBy = "sykdomGrunnlag", cascade = CascadeType.ALL)
    private List<SykdomSøktPeriode> søktePerioder = new ArrayList<>();

    //TODO: Hva skiller denne fra relevanteSøknadsperioder? Kan vi kverke denne også?
    @OneToMany(mappedBy = "sykdomGrunnlag", cascade = CascadeType.ALL)
    private List<SykdomRevurderingPeriode> revurderingPerioder = new ArrayList<>();

    //TODO: AnvendteVurderinger?
    @OneToMany()
    @JoinTable(
        name="SYKDOM_GRUNNLAG_VURDERING",
        joinColumns = @JoinColumn( name="SYKDOM_GRUNNLAG_ID"),
        inverseJoinColumns = @JoinColumn( name="SYKDOM_VURDERING_VERSJON_ID")
    )
    private List<SykdomVurderingVersjon> vurderinger = new ArrayList<>();

    @OneToMany()
    @JoinTable(
        name="SYKDOM_GRUNNLAG_DOKUMENT", //TODO: Et navn som forklarer relasjonen? Er det subsettet av dokumenter som
        // blir klassifisert som legeerklæringer i denne behandlingen? I så fall bør vi vel heller peke på sykdom_dokument_informasjon?
        // Utfra strukturen, kan det tolkes som alle dokumenter som er klassifisert som legeerklæringer til og med denne behandlingen..?
        joinColumns = @JoinColumn( name="SYKDOM_GRUNNLAG_ID"),
        inverseJoinColumns = @JoinColumn( name="SYKDOM_DOKUMENT_ID")
    )
    private List<SykdomDokument> godkjenteLegeerklæringer = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "SYKDOM_INNLEGGELSER_ID")
    private SykdomInnleggelser innleggelser;

    @OneToOne
    @JoinColumn(name = "SYKDOM_DIAGNOSEKODER_ID")
    private SykdomDiagnosekoder diagnosekoder;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR


    SykdomGrunnlag() {}

    public SykdomGrunnlag(UUID sykdomGrunnlagUUID,
            List<SykdomSøktPeriode> søktePerioder,
            List<SykdomRevurderingPeriode> revurderingPerioder,
            List<SykdomVurderingVersjon> vurderinger,
            List<SykdomDokument> godkjenteLegeerklæringer,
            SykdomInnleggelser innleggelser,
            SykdomDiagnosekoder diagnosekoder,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.sykdomGrunnlagUUID = sykdomGrunnlagUUID;
        setSøktePerioder(søktePerioder);
        setRevurderingPerioder(revurderingPerioder);
        this.vurderinger = vurderinger;
        this.godkjenteLegeerklæringer = godkjenteLegeerklæringer;
        this.innleggelser = innleggelser;
        this.diagnosekoder = diagnosekoder;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    private void setSøktePerioder(List<SykdomSøktPeriode> søktePerioder) {
        this.søktePerioder = søktePerioder;
        søktePerioder.forEach(p -> p.setSykdomGrunnlag(this));
    }

    private void setRevurderingPerioder(List<SykdomRevurderingPeriode> revurderingPerioder) {
        this.revurderingPerioder = revurderingPerioder;
        revurderingPerioder.forEach(p -> p.setSykdomGrunnlag(this));
    }

    public List<SykdomSøktPeriode> getSøktePerioder() {
        return Collections.unmodifiableList(søktePerioder);
    }

    public UUID getReferanse() {
        return sykdomGrunnlagUUID;
    }

    public void setSykdomGrunnlagUUID(UUID sykdomGrunnlagUUID) {
        this.sykdomGrunnlagUUID = sykdomGrunnlagUUID;
    }

    public List<SykdomVurderingVersjon> getVurderinger() {
        return vurderinger;
    }

    public void setVurderinger(List<SykdomVurderingVersjon> vurderinger) {
        this.vurderinger = vurderinger;
    }

    public List<SykdomDokument> getGodkjenteLegeerklæringer() {
        if (godkjenteLegeerklæringer == null) {
            return List.of();
        }
        return godkjenteLegeerklæringer;
    }

    public SykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(SykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }

    public SykdomDiagnosekoder getDiagnosekoder() {
        return diagnosekoder;
    }

    public List<String> getSammenlignbarDiagnoseliste() {
        if (diagnosekoder != null) {
            return getDiagnosekoder().getDiagnosekoder().stream().map(SykdomDiagnosekode::getDiagnosekode).sorted().collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public void setDiagnosekoder(SykdomDiagnosekoder diagnosekoder) {
        this.diagnosekoder = diagnosekoder;
    }

    public String getOpprettetAv() {
        return opprettetAv;
    }

    public void setOpprettetAv(String opprettetAv) {
        this.opprettetAv = opprettetAv;
    }

    public LocalDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public void setOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    public List<SykdomRevurderingPeriode> getRevurderingPerioder() {
        return revurderingPerioder;
    }

    Long getId() {
        return id;
    }
}
