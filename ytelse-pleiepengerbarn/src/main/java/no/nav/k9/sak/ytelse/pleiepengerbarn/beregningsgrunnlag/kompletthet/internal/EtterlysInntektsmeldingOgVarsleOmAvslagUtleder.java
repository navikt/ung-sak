package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.KompletthetsAksjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.PeriodeMedMangler;

class EtterlysInntektsmeldingOgVarsleOmAvslagUtleder {

    KompletthetsAksjon utled(EtterlysningInput input) {
        Objects.requireNonNull(input);

        var aksjonspunkter = input.getAksjonspunkter();
        var relevanteMangler = input.getRelevanteMangler()
            .stream()
            .filter(PeriodeMedMangler::harMangler)
            .collect(Collectors.toList());

        var etterlysImAutopunkt = aksjonspunkter.entrySet().stream()
            .filter(it -> AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING.equals(it.getKey()))
            .findAny();
        var eksisterendeFrist = etterlysImAutopunkt.map(Map.Entry::getValue).orElse(null);

        var harIkkeAutopunktFraFør = etterlysImAutopunkt.isEmpty();
        var erIkkeKomplett = !relevanteMangler.isEmpty();

        if ((harIkkeAutopunktFraFør || harEksisterendeFristSomIkkeErUtløpt(eksisterendeFrist)) && erIkkeKomplett) {
            var manglerSomIkkeHarBlittEtterlystEnda = utledManglerSomIkkeHarBlittEtterlystEnda(input);
            var harEtterlystAltTidligere = manglerSomIkkeHarBlittEtterlystEnda.isEmpty();
            if (harIkkeAutopunktFraFør && harEtterlystAltTidligere) {
                return KompletthetsAksjon.uavklart();
            }
            var fristTid = FristKalkulerer.regnUtFrist(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING, eksisterendeFrist);

            return KompletthetsAksjon.automatiskEtterlysning(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING, fristTid, manglerSomIkkeHarBlittEtterlystEnda, DokumentMalType.ETTERLYS_INNTEKTSMELDING_PURRING);
        } else if (!harEksisterendeFristSomIkkeErUtløpt(eksisterendeFrist) && erIkkeKomplett) {
            return KompletthetsAksjon.uavklart();
        }

        return KompletthetsAksjon.fortsett();
    }

    private List<PeriodeMedMangler> utledManglerSomIkkeHarBlittEtterlystEnda(EtterlysningInput input) {
        return input.getRelevanteFiltrerteMangler(DokumentMalType.ETTERLYS_INNTEKTSMELDING_PURRING)
            .stream()
            .filter(PeriodeMedMangler::harMangler)
            .collect(Collectors.toList());
    }

    private boolean harEksisterendeFristSomIkkeErUtløpt(LocalDateTime eksisterendeFrist) {
        return eksisterendeFrist != null && LocalDateTime.now().isBefore(eksisterendeFrist);
    }

}
