package no.nav.k9.sak.inngangsvilkår.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsgrunnlag;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsvilkår;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultOpptjeningsVilkårTjeneste implements OpptjeningsVilkårTjeneste {
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;

    public DefaultOpptjeningsVilkårTjeneste() {
    }

    @Inject
    public DefaultOpptjeningsVilkårTjeneste(OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste) {
        this.opptjeningTjeneste = opptjeningTjeneste;
    }


    @Override
    public VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        Long behandlingId = behandlingReferanse.getBehandlingId();
        AktørId aktørId = behandlingReferanse.getAktørId();

        List<OpptjeningAktivitetPeriode> relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, periode);
        List<OpptjeningInntektPeriode> relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, periode.getFomDato());
        Opptjening opptjening = opptjeningTjeneste.hentOpptjening(behandlingId).finnOpptjening(periode.getFomDato()).orElseThrow();

        LocalDate behandlingstidspunkt = LocalDate.now();

        Opptjeningsgrunnlag grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
            opptjening.getTom())
            .mapTilGrunnlag(relevanteOpptjeningAktiveter, relevanteOpptjeningInntekter);

        //TODO(OJR) overstyrer konfig for fp... burde blitt flyttet ut til konfig verdier.. både for FP og for SVP???
        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        grunnlag.setSkalGodkjenneBasertPåAntatt(false);
        grunnlag.setPeriodeAntattGodkjentFørBehandlingstidspunkt(Period.ofMonths(2));

        // returner egen output i tillegg for senere lagring
        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new Opptjeningsvilkår().evaluer(grunnlag, output);

        VilkårData vilkårData = new VilkårUtfallOversetter().oversett(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag, periode);
        vilkårData.setEkstraVilkårresultat(output);

        return vilkårData;
    }
}
