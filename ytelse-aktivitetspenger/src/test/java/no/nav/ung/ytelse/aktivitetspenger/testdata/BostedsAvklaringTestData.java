package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.typer.Periode;

/**
 * Testdata for en bostedsavklaring gjort av saksbehandler. Lagres på behandlingens bostedsgrunnlag av
 * {@link AktivitetspengerTestScenarioBuilder}.
 */
public record BostedsAvklaringTestData(
    Periode periode,
    Avklaringtype avklaringtype,
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {

    public static BostedsAvklaringTestData opphør(Periode periode, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {
        return new BostedsAvklaringTestData(periode, Avklaringtype.OPPHØR, ikkeOppfyltÅrsak);
    }

    public static BostedsAvklaringTestData avslag(Periode periode, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak) {
        return new BostedsAvklaringTestData(periode, Avklaringtype.AVSLAG, ikkeOppfyltÅrsak);
    }
}
