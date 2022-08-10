package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk;

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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDiagnose;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDiagnoser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomInnleggelser;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomVurderingVersjon;

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
    private List<MedisinskGrunnlagsdataSøktPeriode> søktePerioder = new ArrayList<>();

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

    @Column(name = "SYKDOM_HAR_ANDRE_MEDISINSKE_OPPLYSNINGER", nullable = false)
    private boolean harAndreMedisinskeOpplysninger;

    @OneToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_INNLEGGELSER_ID")
    private PleietrengendeSykdomInnleggelser innleggelser;

    @OneToOne
    @JoinColumn(name = "PLEIETRENGENDE_SYKDOM_DIAGNOSER_ID")
    private PleietrengendeSykdomDiagnoser diagnoser;

    @DiffIgnore
    @Column(name = "OPPRETTET_AV", nullable = false, updatable=false)
    private String opprettetAv;

    @DiffIgnore
    @Column(name = "OPPRETTET_TID", nullable = false, updatable=false)
    private LocalDateTime opprettetTidspunkt; // NOSONAR


    MedisinskGrunnlagsdata() {}

    public MedisinskGrunnlagsdata(
            UUID sykdomGrunnlagUUID,
            List<MedisinskGrunnlagsdataSøktPeriode> søktePerioder,
            List<PleietrengendeSykdomVurderingVersjon> vurderinger,
            List<PleietrengendeSykdomDokument> godkjenteLegeerklæringer,
            boolean harAndreMedisinskeOpplysninger,
            PleietrengendeSykdomInnleggelser innleggelser,
            PleietrengendeSykdomDiagnoser diagnoser,
            String opprettetAv,
            LocalDateTime opprettetTidspunkt) {
        this.sykdomGrunnlagUUID = sykdomGrunnlagUUID;
        setSøktePerioder(søktePerioder);
        this.vurderinger = vurderinger;
        this.godkjenteLegeerklæringer = godkjenteLegeerklæringer;
        this.harAndreMedisinskeOpplysninger = harAndreMedisinskeOpplysninger;
        this.innleggelser = innleggelser;
        this.diagnoser = diagnoser;
        this.opprettetAv = opprettetAv;
        this.opprettetTidspunkt = opprettetTidspunkt;
    }

    private void setSøktePerioder(List<MedisinskGrunnlagsdataSøktPeriode> søktePerioder) {
        this.søktePerioder = søktePerioder;
        søktePerioder.forEach(p -> p.setSykdomGrunnlag(this));
    }

    public List<MedisinskGrunnlagsdataSøktPeriode> getSøktePerioder() {
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

    public boolean isHarAndreMedisinskeOpplysninger() {
        return harAndreMedisinskeOpplysninger;
    }

    public PleietrengendeSykdomInnleggelser getInnleggelser() {
        return innleggelser;
    }

    public void setInnleggelser(PleietrengendeSykdomInnleggelser innleggelser) {
        this.innleggelser = innleggelser;
    }

    public PleietrengendeSykdomDiagnoser getDiagnoser() {
        return diagnoser;
    }

    public List<String> getSammenlignbarDiagnoseliste() {
        if (diagnoser != null) {
            return getDiagnoser().getDiagnoser().stream().map(PleietrengendeSykdomDiagnose::getDiagnosekode).sorted().collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public void setDiagnoser(PleietrengendeSykdomDiagnoser diagnoser) {
        this.diagnoser = diagnoser;
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

    Long getId() {
        return id;
    }
}
