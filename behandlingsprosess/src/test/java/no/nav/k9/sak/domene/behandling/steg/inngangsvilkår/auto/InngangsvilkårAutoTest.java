package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.auto;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsPeriode;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;

@EnableRuleMigrationSupport
public class InngangsvilkårAutoTest {

    private static final ObjectMapper OM = JsonUtil.getObjectMapper();
    private static final List<VilkårType> VILKÅR = Arrays.asList(
        VilkårType.MEDLEMSKAPSVILKÅRET,
        VilkårType.OPPTJENINGSPERIODEVILKÅR,
        VilkårType.OPPTJENINGSVILKÅRET);

    public static Stream<Arguments> provideArguments(){
        List<Arguments> params = new ArrayList<>();
        for(var v : VILKÅR){
            var files = VilkårVurdering.listAllFiles(v);
            var inputFiles = VilkårVurdering.listInputFiles(files);
            for (var in : inputFiles) {
                var out = VilkårVurdering.finnOutputFil(files, in);
                params.add(Arguments.of( v, in, out.orElse(null) ));
            }
        }
        return params.stream();
    }

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void vurder_vilkår(VilkårType v, File in, File out) throws Exception {

        switch (v) {
            case MEDLEMSKAPSVILKÅRET:
                vurderVilkår(VilkårVurdering.DO_NOTHING, v, in, out);
                return;
            case OPPTJENINGSPERIODEVILKÅR:
                fastsettOpptjeningsPeriode(v, in, out);
                return;
            case OPPTJENINGSVILKÅRET:
                vurderOpptjening(v, in, out);
                return;
            default:
                throw new UnsupportedOperationException("" + v);
        }

    }

    private void vurderVilkår(BiConsumer<VilkårResultat, Object> ekstraSjekk, VilkårType v, File in, File out) throws IOException {

        new VilkårVurdering().vurderCase(collector, ekstraSjekk, VilkårVurdering.getVilkårImplementasjon(v), OM, in, out);

    }

    public void vurderOpptjening( VilkårType v, File in, File out) throws IOException {

        final BiConsumer<VilkårResultat, Object> extraDataSjekk = (resultat, extra) -> {

            collector.checkThat("Avvik i opptjeningstid for " + resultat,  ((OpptjeningsvilkårResultat) extra).getResultatOpptjent(), equalTo(Period.parse(resultat.getOpptjentTid())));

        };

        vurderVilkår(extraDataSjekk, v, in, out);
    }

    public void fastsettOpptjeningsPeriode( VilkårType v, File in, File out) throws IOException  {

        final BiConsumer<VilkårResultat, Object> extraDataSjekk = (resultat, extra) -> {

            collector.checkThat("Avvik i opptjeningstid for " + resultat, ((OpptjeningsPeriode) extra).getOpptjeningsperiodeFom(), equalTo(resultat.getOpptjeningFom()));

            collector.checkThat("Avvik i opptjeningstid for " + resultat,  ((OpptjeningsPeriode) extra).getOpptjeningsperiodeTom(), equalTo(resultat.getOpptjeningTom()));

        };

        vurderVilkår(extraDataSjekk, v, in, out);
    }
}
