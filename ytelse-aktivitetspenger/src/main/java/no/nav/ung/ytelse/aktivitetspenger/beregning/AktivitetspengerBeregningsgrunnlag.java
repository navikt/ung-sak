package no.nav.ung.ytelse.aktivitetspenger.beregning;

import jakarta.persistence.*;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.diff.ChangeTracked;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPerioder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.*;

@Entity(name = "AktivitetspengerBeregningsgrunnlag")
@Table(name = "AVP_GR_BEREGNINGSGRUNNLAG")
@DynamicInsert
@DynamicUpdate
public class AktivitetspengerBeregningsgrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AVP_GR_BEREGNINGSGRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "avp_sats_perioder_id", updatable = false)
    private AktivitetspengerSatsPerioder satsperioder;

    @ChangeTracked
    @ManyToMany
    @JoinTable(
        name = "BEREGNINGSGRUNNLAG_KOBLING",
        joinColumns = @JoinColumn(name = "avp_gr_beregningsgrunnlag_id"),
        inverseJoinColumns = @JoinColumn(name = "beregningsgrunnlag_id")
    )
    private List<Beregningsgrunnlag> beregningsgrunnlag = new ArrayList<>();

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public AktivitetspengerBeregningsgrunnlag() {
    }

    public AktivitetspengerSatsPerioder getSatsperioder() {
        return satsperioder;
    }

    void setSatsperioder(AktivitetspengerSatsPerioder grunnsatser) {
        this.satsperioder = grunnsatser;
    }

    public LocalDateTimeline<AktivitetspengerSatsGrunnlag> hentSatsTidslinje() {
        if (satsperioder == null) {
            return LocalDateTimeline.empty();
        }
        var segmenter = satsperioder.getPerioder().stream().map(p ->
            new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.satsGrunnlag())
        ).toList();
        return new LocalDateTimeline<>(segmenter);
    }

    public LocalDateTimeline<Beregningsgrunnlag> hentBeregningsgrunnlagTidslinje() {
        if (beregningsgrunnlag.isEmpty()) {
            throw new IllegalStateException("Fant ikke beregningsgrunnlag på aktivitetspenger beregningsgrunnlag");
        }
        var segmenter = beregningsgrunnlag.stream()
            .map(bg -> new LocalDateSegment<>(bg.getSkjæringstidspunkt(), null, bg))
            .toList();
        return new LocalDateTimeline<>(segmenter);
    }

    public LocalDateTimeline<AktivitetspengerSatser> hentAktivitetspengerSatsTidslinje() {
        return hentBeregningsgrunnlagTidslinje().combine(
            hentSatsTidslinje(),
            (interval, bg, satser) ->
                new LocalDateSegment<>(interval, new AktivitetspengerSatser(satser.getValue(), bg.getValue())),
            LocalDateTimeline.JoinStyle.INNER_JOIN
        );
    }

    public List<Beregningsgrunnlag> getBeregningsgrunnlag() {
        return Collections.unmodifiableList(beregningsgrunnlag);
    }

    public Optional<Beregningsgrunnlag> getSenesteBeregningsgrunnlag() {
        return beregningsgrunnlag.stream().max(Comparator.comparing(Beregningsgrunnlag::getSkjæringstidspunkt));
    }

    void setBeregningsgrunnlag(List<Beregningsgrunnlag> beregningsgrunnlag) {
        this.beregningsgrunnlag = new ArrayList<>(beregningsgrunnlag);
    }

    void setIkkeAktivt() {
        this.aktiv = false;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }
}

