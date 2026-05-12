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

@Entity(name = "AktivitetspengerGrunnlag")
@Table(name = "GR_AVP")
@DynamicInsert
@DynamicUpdate
public class AktivitetspengerGrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_AVP")
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
        joinColumns = @JoinColumn(name = "gr_avp_id"),
        inverseJoinColumns = @JoinColumn(name = "beregningsgrunnlag_id")
    )
    private List<Beregningsgrunnlag> beregningsgrunnlag = new ArrayList<>();

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public AktivitetspengerGrunnlag() {
    }

    public AktivitetspengerSatsPerioder getSatsperioder() {
        return satsperioder;
    }

    void setSatsperioder(AktivitetspengerSatsPerioder grunnsatser) {
        this.satsperioder = grunnsatser;
    }

    public LocalDateTimeline<AktivitetspengerSatsGrunnlag> hentSatsTidslinje() {
        if (satsperioder == null) {
            throw new IllegalStateException("Fant ikke satsperioder på AktivitetspengerGrunnlag");
        }
        var segmenter = satsperioder.getPerioder().stream().map(p ->
            new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), p.satsGrunnlag())
        ).toList();
        return new LocalDateTimeline<>(segmenter);
    }

    public LocalDateTimeline<Beregningsgrunnlag> hentBeregningsgrunnlagTidslinje() {
        if (beregningsgrunnlag.isEmpty()) {
            throw new IllegalStateException("Fant ikke beregningsgrunnlag på AktivitetspengerGrunnlag");
        }
        var segmenter = beregningsgrunnlag.stream()
            .map(bg -> new LocalDateSegment<>(bg.getSkjæringstidspunkt(), null, bg))
            .toList();
        return new LocalDateTimeline<>(segmenter);
    }

    public LocalDateTimeline<AktivitetspengerSatser> hentAktivitetspengerSatsTidslinje() {
        var beregningsgrunnlagTidslinje = hentBeregningsgrunnlagTidslinje();
        var satsTidslinje = hentSatsTidslinje();

        if (!satsTidslinje.disjoint(beregningsgrunnlagTidslinje).isEmpty()) {
            throw new IllegalStateException("Satsperioder finnes utenfor beregningsgrunnlagperioder — beregningsgrunnlagTidslinje må forlenges!");
        }

        return beregningsgrunnlagTidslinje.combine(
            satsTidslinje,
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

