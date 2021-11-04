package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag.kompletthet.internal;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
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
            .filter(it -> AksjonspunktKodeDefinisjon.ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING_KODE.equals(it.getKey()))
            .findAny();
        var eksisterendeFrist = etterlysImAutopunkt.map(Map.Entry::getValue).orElse(null);

        if ((etterlysImAutopunkt.isEmpty() || (eksisterendeFrist != null && LocalDateTime.now().isBefore(eksisterendeFrist))) && !relevanteMangler.isEmpty()) {
            var fristTid = FristKalkulerer.regnUtFrist(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING, eksisterendeFrist);

            return KompletthetsAksjon.automatiskEtterlysning(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYS_IM_VARSLE_AVSLAG_FOR_BEREGNING, fristTid, relevanteMangler);
        } else if((eksisterendeFrist != null && LocalDateTime.now().isAfter(eksisterendeFrist) && !relevanteMangler.isEmpty())) {
            return KompletthetsAksjon.uavklart();
        }

        return KompletthetsAksjon.fortsett();
    }
}
