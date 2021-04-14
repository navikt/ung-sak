package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectReader;

import no.nav.abakus.iaygrunnlag.IayGrunnlagJsonMapper;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.InntektsmeldingSøktePerioderMapper;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.VurderSøknadsfrist;

public class TrekkUtFraværTjenesteTest {

    private static final ObjectReader reader = IayGrunnlagJsonMapper.getMapper().reader();

    @Test
    void skal_trekke_ut_samlet_fravær_riktig() throws Exception {

        var inntektsmeldinger = parseInntektsmeldinger("01");

        var trekkUt = new TrekkUtOppgittFraværPeriode(new VurderSøknadsfrist(false), new InntektsmeldingSøktePerioderMapper());

        var kravPerioder = trekkUt.mapFra(new LinkedHashSet<>(inntektsmeldinger), Map.of());

        System.out.println(kravPerioder);

    }

    @SuppressWarnings("resource")
    private List<Inntektsmelding> parseInntektsmeldinger(String path) throws IOException {
        String basePath = getClass().getSimpleName() + "/" + path + "/";
        List<String> files = IOUtils.readLines(getClass().getClassLoader().getResourceAsStream(basePath), Charsets.UTF_8)
            .stream()
            .map(f -> basePath + f).collect(Collectors.toList());

        return files.stream().map(f -> {
            try {
                return reader.readValue(TrekkUtFraværTjenesteTest.class.getResourceAsStream("/" + f), Inntektsmelding.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }).sorted(Comparator.comparing(Inntektsmelding::getKanalreferanse))
            .collect(Collectors.toList());

    }
}
