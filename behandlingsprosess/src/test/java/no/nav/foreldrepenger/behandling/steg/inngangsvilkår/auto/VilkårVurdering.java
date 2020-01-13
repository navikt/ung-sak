package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.auto;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.Medlemskapsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsvilkår;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.RegelFastsettOpptjeningsperiode;
import no.nav.fpsak.nare.RuleService;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.vedtak.util.Tuple;

class VilkårVurdering {

    private static final Logger log = LoggerFactory.getLogger(VilkårVurdering.class);
    public static final BiConsumer<VilkårResultat, Object> DO_NOTHING = (res, obj) -> {
    };

    void vurderVilkår(ErrorCollector collector, VilkårType vilkår, BiConsumer<VilkårResultat, Object> extraDataValidering) {
        var files = listAllFiles(vilkår);
        vurderCaser(collector, vilkår, extraDataValidering, files);
    }

    public static List<File> listAllFiles(VilkårType vilkår) {
        Objects.requireNonNull(vilkår, "vilkår");
        final File vilkårMappe = new File("src/test/testscript/vilkår/" + vilkår.getKode() + "/");
        final File[] files = vilkårMappe.listFiles();
        return Arrays.asList(files);
    }

    void vurderVilkår(ErrorCollector collector, VilkårType vilkår) {
        vurderVilkår(collector, vilkår, DO_NOTHING);
    }

    private void vurderCaser(ErrorCollector collector, VilkårType vilkår, BiConsumer<VilkårResultat, Object> extraDataValidering, Collection<File> files) {
        if (files != null) {
            final List<File> inputFiler = listInputFiles(files);
            log.info("Sjekker " + vilkår.getKode() + ", fant " + inputFiler.size() + " testcaser.");

            final Tuple<Tuple<Class<Object>, Object>, RuleService<Object>> vilkårRegel = getVilkårImplementasjon(vilkår);
            final ObjectMapper mapper = JsonUtil.getObjectMapper();
            for (File inputFile : inputFiler) {
                vurderCase(collector, extraDataValidering, files, vilkårRegel, mapper, inputFile);
            }
        } else {
            log.warn("Fant ingen testcaser for " + vilkår.getKode());
        }
    }

    public static List<File> listInputFiles(Collection<File> files) {
        return files.stream().filter(it -> it.getName().endsWith(JsonUtil.INPUT_SUFFIX)).collect(Collectors.toList());
    }

    public void vurderCase(ErrorCollector collector,
                           BiConsumer<VilkårResultat, Object> extraDataValidering, 
                           Collection<File> files,
                           final Tuple<Tuple<Class<Object>, Object>, 
                           RuleService<Object>> vilkårRegel, 
                           final ObjectMapper mapper, 
                           File inputFile) {
        try {
            final Optional<File> outputFile = finnOutputFil(files, inputFile);
            vurderCase(collector, extraDataValidering, vilkårRegel, mapper, inputFile, outputFile.orElse(null));
        } catch (IOException e) {
            log.error("Noe uventet gikk galt under parsing av '" + inputFile.getName() + "' : " + e.getMessage(), e);
            collector.addError(e);
        }
    }

    public void vurderCase(ErrorCollector collector, 
                           BiConsumer<VilkårResultat, Object> extraDataValidering,
                           final Tuple<Tuple<Class<Object>, Object>, RuleService<Object>> vilkårRegel, 
                           final ObjectMapper mapper, 
                           File inputFile,
                           File outputFile)
            throws IOException, JsonParseException, JsonMappingException {
        final Tuple<Class<Object>, Object> vilkårObjectClasses = vilkårRegel.getElement1();

        final Object input = mapper.readValue(inputFile, vilkårObjectClasses.getElement1());

        if (outputFile!=null) {
            final VilkårResultat vilkårResultat = mapper.readValue(outputFile, VilkårResultat.class);

            settFunksjonellTidTilKjøretidspunkt(vilkårResultat);

            final Evaluation evaluer;
            final Object resultatObject = vilkårObjectClasses.getElement2();
            if (resultatObject instanceof NoneObject) {
                evaluer = vilkårRegel.getElement2().evaluer(input);
            } else {
                evaluer = vilkårRegel.getElement2().evaluer(input, resultatObject);
            }

            final EvaluationSummary evaluationSummary = new EvaluationSummary(evaluer);
            log.info("Vurderer " + vilkårResultat);

            collector.checkThat("Vurdering av " + inputFile.getName() + " ga ikke forventet resultat.",
                getVilkårUtfallType(evaluationSummary), equalTo(vilkårResultat.getUtfall()));
            extraDataValidering.accept(vilkårResultat, resultatObject);
        } else {
            log.warn("Fant ikke output for evaluering av " + inputFile.getName());
            collector.addError(new FileNotFoundException("Fant ikke output for evaluering av " + inputFile.getName()));
        }
        System.setProperty("funksjonelt.tidsoffset.offset", "P0D");
    }

    private void settFunksjonellTidTilKjøretidspunkt(VilkårResultat vilkårResultat) {
        final Period periode = Period.between(LocalDate.now(), vilkårResultat.getKjøreTidspunkt());
        System.setProperty("funksjonelt.tidsoffset.offset", periode.toString());
    }

    public static Optional<File> finnOutputFil(Collection<File> files, File inputFile) {
        return files.stream()
            .filter(it -> it.getName().endsWith(inputFile.getName().replace(JsonUtil.INPUT_SUFFIX, JsonUtil.OUTPUT_SUFFIX)))
            .findAny(); // Skal bare være en
    }

    private String getVilkårUtfallType(EvaluationSummary summary) {
        Collection<Evaluation> leafEvaluations = summary.leafEvaluations();
        for (Evaluation ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                Resultat res = ev.result();
                switch (res) {
                    case JA:
                        return VilkårUtfallType.OPPFYLT.getKode();
                    case NEI:
                        return VilkårUtfallType.IKKE_OPPFYLT.getKode();
                    case IKKE_VURDERT:
                        return VilkårUtfallType.IKKE_VURDERT.getKode();
                    default:
                        throw new IllegalArgumentException("Ukjent Resultat:" + res + " ved evaluering av:" + ev);
                }
            } else {
                return VilkårUtfallType.OPPFYLT.getKode();
            }
        }

        throw new IllegalArgumentException("leafEvaluations.isEmpty():" + leafEvaluations);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, X> Tuple<Tuple<Class<T>, X>, RuleService<T>> getVilkårImplementasjon(VilkårType vilkår) {
        if (VilkårType.MEDLEMSKAPSVILKÅRET.equals(vilkår)) {
            return new Tuple(new Tuple<>(MedlemskapsvilkårGrunnlag.class, new NoneObject()), new Medlemskapsvilkår());
        }
        if (VilkårType.OPPTJENINGSVILKÅRET.equals(vilkår)) {
            return new Tuple(new Tuple<>(Opptjeningsgrunnlag.class, new OpptjeningsvilkårResultat()), new Opptjeningsvilkår());
        }
        if (VilkårType.OPPTJENINGSPERIODEVILKÅR.equals(vilkår)) {
            return new Tuple(new Tuple<>(OpptjeningsperiodeGrunnlag.class, new OpptjeningsPeriode()), new RegelFastsettOpptjeningsperiode());
        }
        throw new IllegalArgumentException("Støtter ikke vilkår: " + vilkår);
    }

    private static class NoneObject {
    }
}
