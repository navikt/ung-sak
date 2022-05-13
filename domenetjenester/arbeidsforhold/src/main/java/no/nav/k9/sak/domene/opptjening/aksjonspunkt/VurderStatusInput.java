package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderStatusInput {
    private OpptjeningAktivitetType type;
    private BehandlingReferanse behandlingReferanse;
    private DatoIntervallEntitet vilkårsperiode;
    private Yrkesaktivitet registerAktivitet;
    private DatoIntervallEntitet aktivitetPeriode;
    private Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse = new HashMap<>();

    public VurderStatusInput(OpptjeningAktivitetType type, BehandlingReferanse behandlingReferanse) {
        this.type = Objects.requireNonNull(type);
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse);
    }

    public OpptjeningAktivitetType getType() {
        return type;
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public DatoIntervallEntitet getVilkårsperiode() {
        return vilkårsperiode;
    }

    public void setVilkårsperiode(DatoIntervallEntitet vilkårsperiode) {
        this.vilkårsperiode = vilkårsperiode;
    }

    public Yrkesaktivitet getRegisterAktivitet() {
        return registerAktivitet;
    }

    public void setRegisterAktivitet(Yrkesaktivitet yrkesaktivitet) {
        this.registerAktivitet = yrkesaktivitet;
    }

    public DatoIntervallEntitet getAktivitetPeriode() {
        return aktivitetPeriode;
    }

    public void setAktivitetPeriode(DatoIntervallEntitet aktivitetPeriode) {
        this.aktivitetPeriode = aktivitetPeriode;
    }

    public Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> getTidslinjePerYtelse() {
        return tidslinjePerYtelse;
    }

    public void setTidslinjePerYtelse(Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePerYtelse) {
        this.tidslinjePerYtelse = tidslinjePerYtelse;
    }
}
