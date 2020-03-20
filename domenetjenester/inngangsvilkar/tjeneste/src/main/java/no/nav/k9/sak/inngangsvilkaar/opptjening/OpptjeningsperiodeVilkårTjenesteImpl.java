package no.nav.k9.sak.inngangsvilkaar.opptjening;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkaar.VilkårData;
import no.nav.k9.sak.inngangsvilkaar.impl.InngangsvilkårOversetter;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.k9.sak.inngangsvilkaar.regelmodell.opptjeningsperiode.RegelFastsettOpptjeningsperiode;

@ApplicationScoped
@FagsakYtelseTypeRef
public class OpptjeningsperiodeVilkårTjenesteImpl implements OpptjeningsperiodeVilkårTjeneste {

    private InngangsvilkårOversetter inngangsvilkårOversetter;
    private Period antallDagerOpptjeningsperiode;

    OpptjeningsperiodeVilkårTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningsperiodeVilkårTjenesteImpl(InngangsvilkårOversetter inngangsvilkårOversetter) {
        this.inngangsvilkårOversetter = inngangsvilkårOversetter;
        this.antallDagerOpptjeningsperiode = Period.ofDays(28);
    }

    @Override
    public VilkårData vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, LocalDate førsteUttaksdato, DatoIntervallEntitet periode) {
        OpptjeningsperiodeGrunnlag grunnlag = new OpptjeningsperiodeGrunnlag();

        grunnlag.setFørsteUttaksDato(førsteUttaksdato);
        grunnlag.setPeriodeLengde(antallDagerOpptjeningsperiode);

        final OpptjeningsPeriode data = new OpptjeningsPeriode();
        Evaluation evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, data);

        VilkårData resultat = inngangsvilkårOversetter.tilVilkårData(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag, periode);
        resultat.setEkstraVilkårresultat(data);
        return resultat;
    }
}
