package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.ung.kodeverk.vilkår.AvklaringKilde;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.sak.typer.Periode;

/**
 * Testdata for en vilkårsavklaring gjort av saksbehandler. Lagres på behandlingens generiske
 * vilkårsavklaringsgrunnlag av {@link AktivitetspengerTestScenarioBuilder}.
 */
public record VilkårsavklaringTestData(
    Periode periode,
    Avklaringtype avklaringtype,
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak,
    AvklaringKilde kilde,
    String kildeFritekst) {

    public static VilkårsavklaringTestData opphør(Periode periode, IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak, AvklaringKilde kilde) {
        return new VilkårsavklaringTestData(periode, Avklaringtype.OPPHØR, ikkeOppfyltÅrsak, kilde, null);
    }

    public static VilkårsavklaringTestData avslag(Periode periode, IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak, AvklaringKilde kilde) {
        return new VilkårsavklaringTestData(periode, Avklaringtype.AVSLAG, ikkeOppfyltÅrsak, kilde, null);
    }

    public VilkårsavklaringTestData medKilde(AvklaringKilde nyKilde, String nyKildeFritekst) {
        return new VilkårsavklaringTestData(periode, avklaringtype, ikkeOppfyltÅrsak, nyKilde, nyKildeFritekst);
    }
}
