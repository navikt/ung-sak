package no.nav.ung.ytelse.aktivitetspenger.beregning;

import jakarta.persistence.*;
import no.nav.ung.sak.diff.ChangeTracked;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
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

