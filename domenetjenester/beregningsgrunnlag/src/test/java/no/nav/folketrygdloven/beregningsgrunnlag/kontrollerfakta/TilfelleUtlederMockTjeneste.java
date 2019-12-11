package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;

import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.ArbeidstakerOgFrilanserISammeOrganisasjonTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.EtterlønnSluttpakkeTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.KortvarigArbeidsforholdTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.KunYtelseTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.NyIArbeidslivetTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.NyoppstartetFLTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.TilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.VurderLønnsendringTilfelleUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere.VurderMottarYtelseTilfelleUtleder;

public class TilfelleUtlederMockTjeneste {

    public static Instance<TilfelleUtleder> getUtlederInstances() {
        @SuppressWarnings("unchecked")
        Instance<TilfelleUtleder> utlederInstances = mock(Instance.class);
        List<TilfelleUtleder> utledere = new ArrayList<>();
        leggTilUtleder(new NyIArbeidslivetTilfelleUtleder(), utledere);
        leggTilUtleder(new KunYtelseTilfelleUtleder(), utledere);
        leggTilUtleder(new NyoppstartetFLTilfelleUtleder(), utledere);
        leggTilUtleder(new VurderLønnsendringTilfelleUtleder(), utledere);
        leggTilUtleder(new KortvarigArbeidsforholdTilfelleUtleder(), utledere);
        leggTilUtleder(new ArbeidstakerOgFrilanserISammeOrganisasjonTilfelleUtleder(), utledere);
        leggTilUtleder(new EtterlønnSluttpakkeTilfelleUtleder(), utledere);
        leggTilUtleder(new VurderMottarYtelseTilfelleUtleder(), utledere);
        when(utlederInstances.iterator()).thenReturn(utledere.iterator());
        when(utlederInstances.stream()).thenReturn(utledere.stream());
        return utlederInstances;
    }

    private static void leggTilUtleder(TilfelleUtleder utleder, List<TilfelleUtleder> utledere) {
        utledere.add(utleder);
    }
}
