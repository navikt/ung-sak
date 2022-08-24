package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;

public class FinnInntekterFraForrigeRegisterinnhenting implements SigruninntekterForBeregningFilter {

    @Override
    public List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate skjæringstidspunkt) {
        var inntektFilter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(behandlingReferanse.getAktørId())).før(skjæringstidspunkt);
        return inntektFilter.getAlleInntektBeregnetSkatt();
    }


}
