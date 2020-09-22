package no.nav.k9.sak.inngangsvilkår.opptjeningsperiode;

import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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

    private static final int DAGER_OPPTJENING = 28;
    private Period antallDagerOpptjeningsperiode;

    @Inject
    public DefaultOpptjeningsperiodeVilkårTjeneste() {
        this.antallDagerOpptjeningsperiode = Period.ofDays(DAGER_OPPTJENING);
    }

    @Override
    public NavigableMap<DatoIntervallEntitet, VilkårData> vurderOpptjeningsperiodeVilkår(BehandlingReferanse behandlingReferanse, Collection<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return Collections.emptyNavigableMap();
        }
        NavigableMap<DatoIntervallEntitet, VilkårData> resultater = new TreeMap<>();
        for (var periode : new TreeSet<>(perioder)) {
            var grunnlag = new OpptjeningsperiodeGrunnlag();

            grunnlag.setFørsteUttaksDato(periode.getFomDato());
            grunnlag.setPeriodeLengde(antallDagerOpptjeningsperiode);

            var outputContainer = new OpptjeningsPeriode();
            var evaluation = new RegelFastsettOpptjeningsperiode().evaluer(grunnlag, outputContainer);

            var resultat = new VilkårUtfallOversetter().oversett(VilkårType.OPPTJENINGSPERIODEVILKÅR, evaluation, grunnlag, periode);
            resultat.setEkstraVilkårresultat(outputContainer);
            resultater.put(periode, resultat);
        }
        return Collections.unmodifiableNavigableMap(resultater);
    }
}
