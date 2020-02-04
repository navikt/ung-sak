package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static no.nav.foreldrepenger.inngangsvilkaar.RegelintegrasjonFeil.FEILFACTORY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;

public class VilkårUtfallOversetter {

    private Logger logger = LoggerFactory.getLogger(VilkårUtfallOversetter.class);

    public VilkårUtfallOversetter() {
    }

    public VilkårData oversett(VilkårType vilkårType, Evaluation evaluation, VilkårGrunnlag grunnlag, DatoIntervallEntitet periode) {
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        String regelEvalueringJson = EvaluationSerializer.asJson(evaluation);

        String jsonGrunnlag;
        try {
            VilkårJsonObjectMapper jsonMapper = new VilkårJsonObjectMapper();
            jsonGrunnlag = jsonMapper.writeValueAsString(grunnlag);
        } catch (IllegalArgumentException e) {
            throw FEILFACTORY.kanIkkeSerialisereRegelinput(vilkårType.getKode(), e).toException();
        }
        // kan hende det ikke burde ligge som info, men er veldig greit i de tilfellene FP ruller tilbake databasen for da har vi fortsatt regel
        // input!!
        logger.info("json grunnlag for " + vilkårType.getKode() + ": " + jsonGrunnlag); // NOSONAR

        Utfall utfall = getVilkårUtfallType(summary);

        Properties merknadParametere = getMerknadParametere(summary);

        List<AksjonspunktDefinisjon> apDefinisjoner = getAksjonspunktDefinisjoner(summary);
        VilkårUtfallMerknad vilkårUtfallMerknad = getVilkårUtfallMerknad(summary);

        return new VilkårData(periode, vilkårType, utfall, merknadParametere, apDefinisjoner, vilkårUtfallMerknad, null,
            regelEvalueringJson, jsonGrunnlag, false);

    }

    private VilkårUtfallMerknad getVilkårUtfallMerknad(EvaluationSummary summary) {
        Collection<Evaluation> leafEvaluations = summary.leafEvaluations();

        if (leafEvaluations.size() > 1) {
            throw new IllegalArgumentException("Supporterer kun et utfall p.t., fikk:" + leafEvaluations);
        } else {
            VilkårUtfallMerknad vilkårUtfallMerknad = null;
            for (Evaluation ev : leafEvaluations) {
                if (ev.getOutcome() != null) {
                    vilkårUtfallMerknad = VilkårUtfallMerknad.fraKode(ev.getOutcome().getReasonCode());
                    break;
                }
            }
            return vilkårUtfallMerknad;
        }
    }

    private List<AksjonspunktDefinisjon> getAksjonspunktDefinisjoner(EvaluationSummary summary) {
        Collection<Evaluation> leafEvaluations = summary.leafEvaluations(Resultat.IKKE_VURDERT);
        List<AksjonspunktDefinisjon> apDefinisjoner = new ArrayList<>(2);
        for (Evaluation ev : leafEvaluations) {
            AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(ev.getOutcome().getReasonCode());
            apDefinisjoner.add(aksjonspunktDefinisjon);
        }
        return apDefinisjoner;
    }

    private Properties getMerknadParametere(EvaluationSummary summary) {
        Properties params = new Properties();
        Collection<Evaluation> leafEvaluations = summary.leafEvaluations();
        for (Evaluation ev : leafEvaluations) {
            Map<String, Object> evalProps = ev.getEvaluationProperties();
            if (evalProps != null) {
                params.putAll(evalProps);
            }
        }
        return params;
    }

    private Utfall getVilkårUtfallType(EvaluationSummary summary) {
        Collection<Evaluation> leafEvaluations = summary.leafEvaluations();
        for (Evaluation ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                Resultat res = ev.result();
                switch (res) {
                    case JA:
                        return Utfall.OPPFYLT;
                    case NEI:
                        return Utfall.IKKE_OPPFYLT;
                    case IKKE_VURDERT:
                        return Utfall.IKKE_VURDERT;
                    default:
                        throw new IllegalArgumentException("Ukjent Resultat:" + res + " ved evaluering av:" + ev);
                }
            } else {
                return Utfall.OPPFYLT;
            }
        }

        throw new IllegalArgumentException("leafEvaluations.isEmpty():" + leafEvaluations);
    }

}
