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

@Entity(name = "MedisinskGrunnlagsdata")
@Table(name = "MEDISINSK_GRUNNLAGSDATA")
public class MedisinskGrunnlagsdata {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDISINSK_GRUNNLAGSDATA")
    private Long id;

    @Column(name = "SPORINGSREFERANSE", nullable = false)
    private UUID sykdomGrunnlagUUID;

    //TODO: Deprekere, hente data rett fra gr_soeknadsperiode i stedet.
    @OneToMany(mappedBy = "medisinskGrunnlagsdata", cascade = CascadeType.ALL)
    private List<SykdomSøktPeriode> søktePerioder = new ArrayList<>();

    //TODO: Hva skiller denne fra relevanteSøknadsperioder? Kan vi kverke denne også?
    @OneToMany(mappedBy = "medisinskGrunnlagsdata", cascade = CascadeType.ALL)
    private List<SykdomRevurderingPeriode> revurderingPerioder = new ArrayList<>();

    //TODO: AnvendteVurderinger?
    @OneToMany()
    @JoinTable(
        name="MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER",
        joinColumns = @JoinColumn( name="MEDISINSK_GRUNNLAGSDATA_ID"),
        inverseJoinColumns = @JoinColumn( name="PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ID")
    )
    private List<PleietrengendeSykdomVurderingVersjon> vurderinger = new ArrayList<>();

    //Liste over alle godkjente legeerklæringer på den pleietrengende idet grunnlaget opprettes.
    @OneToMany()
    @JoinTable(
        name="MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER",
        joinColumns = @JoinColumn( name="MEDISINSK_GRUNNLAGSDATA_ID"),
        inverseJoinColumns = @JoinColumn( name="PLEIETRENGENDE_SYKDOM_DOKUMENT_ID")
    )
    private List<PleietrengendeSykdomDokument> godkjenteLegeerklæringer = new ArrayList<>();

    @OneToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_INNLEGGELSER_ID")
    private PleietrengendeSykdomInnleggelser innleggelser;

    @OneToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_DIAGNOSER_ID")
    private PleietrengendeSykdomDiagnoser diagnosekoder;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR


    MedisinskGrunnlagsdata() {}

    public MedisinskGrunnlagsdata(UUID sykdomGrunnlagUUID,
                                  List<SykdomSøktPeriode> søktePerioder,
                                  List<SykdomRevurderingPeriode> revurderingPerioder,
                                  List<PleietrengendeSykdomVurderingVersjon> vurderinger,
                                  List<PleietrengendeSykdomDokument> godkjenteLegeerklæringer,
                                  PleietrengendeSykdomInnleggelser innleggelser,
                                  PleietrengendeSykdomDiagnoser diagnosekoder,
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

    public List<PleietrengendeSykdomVurderingVersjon> getVurderinger() {
        return vurderinger;
    }

    public void setVurderinger(List<PleietrengendeSykdomVurderingVersjon> vurderinger) {
        this.vurderinger = vurderinger;
    }

    public List<PleietrengendeSykdomDokument> getGodkjenteLegeerklæringer() {
        if (godkjenteLegeerklæringer == null) {
            return List.of();
        }
        return godkjenteLegeerklæringer;
    }

    public PleietrengendeSykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(PleietrengendeSykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }

    public PleietrengendeSykdomDiagnoser getDiagnosekoder() {
        return diagnosekoder;
    }

    public List<String> getSammenlignbarDiagnoseliste() {
        if (diagnosekoder != null) {
            return getDiagnosekoder().getDiagnoser().stream().map(PleietrengendeSykdomDiagnose::getDiagnosekode).sorted().collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public void setDiagnosekoder(PleietrengendeSykdomDiagnoser diagnosekoder) {
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
