package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import static java.util.Collections.emptyList;
import static no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat.opprettListeForAksjonspunkt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@Dependent
public class AksjonspunktUtlederForVurderArbeidsforhold {
    private static final List<AksjonspunktResultat> INGEN_AKSJONSPUNKTER = emptyList();

    private VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste;
    private ArbeidsforholdUtenRelevantOppgittOpptjening arbeidsforholdUtenRelevantOppgittOpptjeningTjeneste;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    AksjonspunktUtlederForVurderArbeidsforhold() {
    }

    @Inject
    public AksjonspunktUtlederForVurderArbeidsforhold(InntektArbeidYtelseTjeneste iayTjeneste,
                                                      VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.vurderArbeidsforholdTjeneste = vurderArbeidsforholdTjeneste;
        this.arbeidsforholdUtenRelevantOppgittOpptjeningTjeneste = new ArbeidsforholdUtenRelevantOppgittOpptjening();
    }

    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        var iayGrunnlag = iayTjeneste.finnGrunnlag(param.getBehandlingId());
        if (iayGrunnlag.isPresent()) {
            var vurder = hentArbeidsforholdTilVurdering(param, iayGrunnlag.get());
            if (!vurder.isEmpty()) {
                return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
            }
        }

        if (arbeidsforholdUtenRelevantOppgittOpptjeningTjeneste.erUtenRelevantOppgittOpptjening(param, iayGrunnlag)) {
            return opprettListeForAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforholdTilVurdering(AksjonspunktUtlederInput param,
                                                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder;
        vurder = vurderArbeidsforholdTjeneste.vurder(param.getRef(), iayGrunnlag);
        return vurder;
    }
}
