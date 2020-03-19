package no.nav.foreldrepenger.inngangsvilkaar.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
@FagsakYtelseTypeRef
public class OpptjeningsVilkårTjenesteImpl implements OpptjeningsVilkårTjeneste {
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;
    private InngangsvilkårOversetter inngangsvilkårOversetter;

    public OpptjeningsVilkårTjenesteImpl() {
    }

    @Inject
    public OpptjeningsVilkårTjenesteImpl(InngangsvilkårOversetter inngangsvilkårOversetter,
                                        OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.opptjeningTjeneste = opptjeningTjeneste;
    }


    @Override
    public VilkårData vurderOpptjeningsVilkår(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        Long behandlingId = behandlingReferanse.getBehandlingId();
        AktørId aktørId = behandlingReferanse.getAktørId();

        List<OpptjeningAktivitetPeriode> relevanteOpptjeningAktiveter = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, periode.getFomDato());
        List<OpptjeningInntektPeriode> relevanteOpptjeningInntekter = opptjeningTjeneste.hentRelevanteOpptjeningInntekterForVilkårVurdering(behandlingId, aktørId, periode.getFomDato());
        Opptjening opptjening = opptjeningTjeneste.hentOpptjening(behandlingId);

        LocalDate behandlingstidspunkt = LocalDate.now();

        Opptjeningsgrunnlag grunnlag = new OpptjeningsgrunnlagAdapter(behandlingstidspunkt, opptjening.getFom(),
            opptjening.getTom())
            .mapTilGrunnlag(relevanteOpptjeningAktiveter, relevanteOpptjeningInntekter);

        //TODO(OJR) overstyrer konfig for fp... burde blitt flyttet ut til konfig verdier.. både for FP og for SVP???
        grunnlag.setMinsteAntallDagerGodkjent(28);
        grunnlag.setMinsteAntallMånederGodkjent(0);
        grunnlag.setMinsteAntallDagerForVent(0);
        grunnlag.setMinsteAntallMånederForVent(0);
        //TODO(OJR) denne burde kanskje endres til false i en revurdering-kontekts i etterkant?
        grunnlag.setSkalGodkjenneBasertPåAntatt(true);
        grunnlag.setPeriodeAntattGodkjentFørBehandlingstidspunkt(Period.ofYears(1));

        // returner egen output i tillegg for senere lagring
        OpptjeningsvilkårResultat output = new OpptjeningsvilkårResultat();
        Evaluation evaluation = new Opptjeningsvilkår().evaluer(grunnlag, output);

        VilkårData vilkårData = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSVILKÅRET, evaluation, grunnlag, periode);
        vilkårData.setEkstraVilkårresultat(output);

        return vilkårData;
    }
}
