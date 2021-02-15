package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultInntektsmeldingerRelevantForBeregning implements InntektsmeldingerRelevantForBeregning {

    @Override
    public List<Inntektsmelding> utledInntektsmeldingerSomGjelderForPeriode(Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        // Sender over alle vi har \o/
        return new ArrayList<>(sakInntektsmeldinger);
    }
}
