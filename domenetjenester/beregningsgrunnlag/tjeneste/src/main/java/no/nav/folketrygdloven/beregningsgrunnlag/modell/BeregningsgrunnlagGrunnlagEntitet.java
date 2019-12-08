package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;


@Entity(name = "BeregningsgrunnlagGrunnlagEntitet")
@Table(name = "GR_BEREGNINGSGRUNNLAG")
public class BeregningsgrunnlagGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BEREGNINGSGRUNNLAG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @OneToOne
    @JoinColumn(name = "beregningsgrunnlag_id", updatable = false, unique = true)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @ManyToOne
    @JoinColumn(name = "register_aktiviteter_id", updatable = false, unique = true)
    private BeregningAktivitetAggregatEntitet registerAktiviteter;

    @ManyToOne
    @JoinColumn(name = "saksbehandlet_aktiviteter_id", updatable = false, unique = true)
    private BeregningAktivitetAggregatEntitet saksbehandletAktiviteter;

    @ManyToOne
    @JoinColumn(name = "ba_overstyringer_id", updatable = false, unique = true)
    private BeregningAktivitetOverstyringerEntitet overstyringer;

    @ManyToOne
    @JoinColumn(name = "br_overstyringer_id", updatable = false, unique = true)
    private BeregningRefusjonOverstyringerEntitet refusjonOverstyringer;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Convert(converter=BeregningsgrunnlagTilstand.KodeverdiConverter.class)
    @Column(name="steg_opprettet", nullable = false)
    private BeregningsgrunnlagTilstand beregningsgrunnlagTilstand;

    public BeregningsgrunnlagGrunnlagEntitet() {
    }

    BeregningsgrunnlagGrunnlagEntitet(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        grunnlag.getBeregningsgrunnlag().ifPresent(this::setBeregningsgrunnlag);
        this.setRegisterAktiviteter(grunnlag.getRegisterAktiviteter());
        grunnlag.getSaksbehandletAktiviteter().ifPresent(this::setSaksbehandletAktiviteter);
        grunnlag.getOverstyring().ifPresent(this::setOverstyringer);
        grunnlag.getRefusjonOverstyringer().ifPresent(this::setRefusjonOverstyringer);
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Optional<BeregningsgrunnlagEntitet> getBeregningsgrunnlag() {
        return Optional.ofNullable(beregningsgrunnlag);
    }

    public BeregningAktivitetAggregatEntitet getRegisterAktiviteter() {
        return registerAktiviteter;
    }

    public Optional<BeregningAktivitetAggregatEntitet> getSaksbehandletAktiviteter() {
        return Optional.ofNullable(saksbehandletAktiviteter);
    }

    public Optional<BeregningAktivitetAggregatEntitet> getOverstyrteEllerSaksbehandletAktiviteter() {
        Optional<BeregningAktivitetAggregatEntitet> overstyrteAktiviteter = getOverstyrteAktiviteter();
        if (overstyrteAktiviteter.isPresent()) {
            return overstyrteAktiviteter;
        }
        return Optional.ofNullable(saksbehandletAktiviteter);
    }

    public Optional<BeregningAktivitetOverstyringerEntitet> getOverstyring() {
        return Optional.ofNullable(overstyringer);
    }

    private Optional<BeregningAktivitetAggregatEntitet> getOverstyrteAktiviteter() {
        if (overstyringer != null) {
            List<BeregningAktivitetEntitet> overstyrteAktiviteter = registerAktiviteter.getBeregningAktiviteter().stream()
                    .filter(beregningAktivitet -> beregningAktivitet.skalBrukes(overstyringer))
                    .collect(Collectors.toList());
            BeregningAktivitetAggregatEntitet.Builder overstyrtBuilder = BeregningAktivitetAggregatEntitet.builder()
                    .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
            overstyrteAktiviteter.forEach(aktivitet -> {
                BeregningAktivitetEntitet kopiert = Kopimaskin.deepCopy(aktivitet);
                overstyrtBuilder.leggTilAktivitet(kopiert);
            });
            return Optional.of(overstyrtBuilder.build());
        }
        return Optional.empty();
    }

    public BeregningAktivitetAggregatEntitet getGjeldendeAktiviteter() {
        return getOverstyrteAktiviteter()
                .or(this::getSaksbehandletAktiviteter)
                .orElse(registerAktiviteter);
    }

    public BeregningAktivitetAggregatEntitet getOverstyrteEllerRegisterAktiviteter() {
        Optional<BeregningAktivitetAggregatEntitet> overstyrteAktiviteter = getOverstyrteAktiviteter();
        if (overstyrteAktiviteter.isPresent()) {
            return overstyrteAktiviteter.get();
        }
        return registerAktiviteter;
    }

    public BeregningsgrunnlagTilstand getBeregningsgrunnlagTilstand() {
        return beregningsgrunnlagTilstand;
    }

    public boolean erAktivt() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    void setRegisterAktiviteter(BeregningAktivitetAggregatEntitet registerAktiviteter) {
        this.registerAktiviteter = registerAktiviteter;
    }

    void setSaksbehandletAktiviteter(BeregningAktivitetAggregatEntitet saksbehandletAktiviteter) {
        this.saksbehandletAktiviteter = saksbehandletAktiviteter;
    }

    void setBeregningsgrunnlagTilstand(BeregningsgrunnlagTilstand beregningsgrunnlagTilstand) {
        this.beregningsgrunnlagTilstand = beregningsgrunnlagTilstand;
    }

    void setOverstyringer(BeregningAktivitetOverstyringerEntitet overstyringer) {
        this.overstyringer = overstyringer;
    }

    public Optional<BeregningRefusjonOverstyringerEntitet> getRefusjonOverstyringer() {
        return Optional.ofNullable(refusjonOverstyringer);
    }

    void setRefusjonOverstyringer(BeregningRefusjonOverstyringerEntitet refusjonOverstyringer) {
        this.refusjonOverstyringer = refusjonOverstyringer;
    }
}
