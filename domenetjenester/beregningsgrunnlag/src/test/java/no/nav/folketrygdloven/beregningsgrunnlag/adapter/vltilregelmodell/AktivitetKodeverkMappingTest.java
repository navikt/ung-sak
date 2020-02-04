package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell;

import java.util.EnumSet;

import org.junit.Rule;
import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapOpptjeningAktivitetTypeFraVLTilRegel;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;

public class AktivitetKodeverkMappingTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Test
    public void skal_verifisere_at_beregningsreglene_kjenner_alle_opptjeningsaktiviteter_i_kodeverk() {
        for (OpptjeningAktivitetType kode : EnumSet.allOf(OpptjeningAktivitetType.class)) {
            //TODO(OJR) skal fjerne UTDANNINGSPERMISJON fra kodeverk
            if (!OpptjeningAktivitetType.UDEFINERT.equals(kode) && !kode.getKode().equals("UTDANNINGSPERMISJON")) {
                MapOpptjeningAktivitetTypeFraVLTilRegel.map(kode);
            }
        }
    }
}
