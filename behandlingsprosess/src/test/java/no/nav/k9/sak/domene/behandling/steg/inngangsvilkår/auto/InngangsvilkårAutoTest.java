package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.auto;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsPeriode;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;

@RunWith(Parameterized.class)
public class InngangsvilkårAutoTest {

    private static final ObjectMapper OM = JsonUtil.getObjectMapper();
    private static final List<VilkårType> VILKÅR = Arrays.asList(
        VilkårType.MEDLEMSKAPSVILKÅRET,
        VilkårType.OPPTJENINGSPERIODEVILKÅR,
        VilkårType.OPPTJENINGSVILKÅRET);

    @org.junit.runners.Parameterized.Parameters(name = "{0}-{1}")
    public static Collection<Object[]> parameters() {
        List<Object[]> params = new ArrayList<>();
        for (var v : VILKÅR) {
            var files = VilkårVurdering.listAllFiles(v);
            var inputFiles = VilkårVurdering.listInputFiles(files);
            for (var in : inputFiles) {
                var out = VilkårVurdering.finnOutputFil(files, in);
                params.add(new Object[] { v, in, out.orElse(null) });
            }
        }
        return params;
    }

    private VilkårType v;
    private File in;
    private File out;

    public InngangsvilkårAutoTest(VilkårType v, File in, File out) {
        this.v = v;
        this.in = in;
        this.out = out;
    }

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Test
    public void vurder_vilkår() throws Exception {

        switch (v) {
            case MEDLEMSKAPSVILKÅRET:
                vurderVilkår(VilkårVurdering.DO_NOTHING);
                return;
            case OPPTJENINGSPERIODEVILKÅR:     
                fastsettOpptjeningsPeriode();
                return;
            case OPPTJENINGSVILKÅRET:
                vurderOpptjening();
                return;
            default:
                throw new UnsupportedOperationException("" + v);
        }

    }

    private void vurderVilkår(BiConsumer<VilkårResultat, Object> ekstraSjekk) throws IOException {
        new VilkårVurdering().vurderCase(collector, ekstraSjekk, VilkårVurdering.getVilkårImplementasjon(v), OM, in, out);
    }

    public void vurderOpptjening() throws IOException {
        final BiConsumer<VilkårResultat, Object> extraDataSjekk = (resultat, extra) -> collector.checkThat("Avvik i opptjeningstid for " + resultat,
            ((OpptjeningsvilkårResultat) extra).getResultatOpptjent(), equalTo(Period.parse(resultat.getOpptjentTid())));
        vurderVilkår(extraDataSjekk);
    }

    public void fastsettOpptjeningsPeriode() throws IOException  {
        final BiConsumer<VilkårResultat, Object> extraDataSjekk = (resultat, extra) -> {
            collector.checkThat("Avvik i opptjeningstid for " + resultat,
                ((OpptjeningsPeriode) extra).getOpptjeningsperiodeFom(), equalTo(resultat.getOpptjeningFom()));
            collector.checkThat("Avvik i opptjeningstid for " + resultat,
                ((OpptjeningsPeriode) extra).getOpptjeningsperiodeTom(), equalTo(resultat.getOpptjeningTom()));
        };
        vurderVilkår(extraDataSjekk);
    }
}
