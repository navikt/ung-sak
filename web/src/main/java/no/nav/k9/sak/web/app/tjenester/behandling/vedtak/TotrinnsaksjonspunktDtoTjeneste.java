package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnsArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnskontrollAksjonspunkterDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.sak.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;

@Dependent
public class TotrinnsaksjonspunktDtoTjeneste {
    private TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste;

    protected TotrinnsaksjonspunktDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public TotrinnsaksjonspunktDtoTjeneste(TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste) {
        this.totrinnArbeidsforholdDtoTjeneste = totrinnArbeidsforholdDtoTjeneste;
    }

    public TotrinnskontrollAksjonspunkterDto lagTotrinnskontrollAksjonspunktDto(Totrinnsvurdering aksjonspunkt,
                                                                                Behandling behandling,
                                                                                Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {

        List<TotrinnsArbeidsforholdDto> totrinnArbeidsforhold = totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, aksjonspunkt,
            totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getGrunnlagUuid));


        return new TotrinnskontrollAksjonspunkterDto.Builder()
            .medAksjonspunktKode(aksjonspunkt.getAksjonspunktDefinisjon())
            .medBesluttersBegrunnelse(aksjonspunkt.getBegrunnelse())
            .medArbeidsforhold(totrinnArbeidsforhold)
            .medTotrinnskontrollGodkjent(aksjonspunkt.isGodkjent())
            .medVurderPaNyttArsaker(hentVurderPåNyttÅrsaker(aksjonspunkt))
            .build();
    }

    private Set<VurderÅrsak> hentVurderPåNyttÅrsaker(Totrinnsvurdering aksjonspunkt) {
        return aksjonspunkt.getVurderPåNyttÅrsaker().stream()
            .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType)
            .collect(Collectors.toSet());
    }
}
