package no.nav.k9.sak.inngangsvilkår.opptjeningsperiode;

import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårData;
import no.nav.k9.sak.inngangsvilkår.VilkårUtfallOversetter;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsPeriode;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsperiodeGrunnlag;
import no.nav.k9.sak.inngangsvilkår.opptjeningsperiode.regelmodell.RegelFastsettOpptjeningsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultOpptjeningsperiodeVilkårTjeneste implements OpptjeningsperiodeVilkårTjeneste {

    private Period antallDagerOpptjeningsperiode;

    @Inject
    public DefaultOpptjeningsperiodeVilkårTjeneste() {
        this.antallDagerOpptjeningsperiode = Period.ofDays(28);
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, DatoIntervallEntitet periode) {
        OpptjeningsperiodeGrunnlag grunnlag = new OpptjeningsperiodeGrunnlag();

        grunnlag.setFørsteUttaksDato(periode.getFomDato());
        grunnlag.setPeriodeLengde(antallDagerOpptjeningsperiode);

        final OpptjeningsPeriode data = new OpptjeningsPeriode();
        Evaluation evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, data);

        VilkårData resultat = new VilkårUtfallOversetter().oversett(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag, periode);
        resultat.setEkstraVilkårresultat(data);
        return resultat;
    }
}
