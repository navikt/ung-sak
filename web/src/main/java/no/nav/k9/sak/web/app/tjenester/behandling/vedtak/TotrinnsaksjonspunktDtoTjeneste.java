package no.nav.k9.sak.web.app.tjenester.behandling.vedtak;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnskontrollAksjonspunkterDto;
import no.nav.k9.sak.kontrakt.vedtak.TotrinnskontrollVurderÅrsak;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.sak.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;

@ApplicationScoped
public class TotrinnsaksjonspunktDtoTjeneste {
    private TotrinnsBeregningDtoTjeneste totrinnsBeregningDtoTjeneste;
    private TotrinnskontrollAktivitetDtoTjeneste totrinnskontrollAktivitetDtoTjeneste;
    private TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste;


    protected TotrinnsaksjonspunktDtoTjeneste() {
        // for CDI proxy
    }


    @Inject
    public TotrinnsaksjonspunktDtoTjeneste(TotrinnsBeregningDtoTjeneste totrinnsBeregningDtoTjeneste,
                                           TotrinnArbeidsforholdDtoTjeneste totrinnArbeidsforholdDtoTjeneste,
                                           TotrinnskontrollAktivitetDtoTjeneste totrinnskontrollAktivitetDtoTjeneste) {
        this.totrinnskontrollAktivitetDtoTjeneste = totrinnskontrollAktivitetDtoTjeneste;
        this.totrinnsBeregningDtoTjeneste = totrinnsBeregningDtoTjeneste;
        this.totrinnArbeidsforholdDtoTjeneste = totrinnArbeidsforholdDtoTjeneste;
    }

    public TotrinnskontrollAksjonspunkterDto lagTotrinnskontrollAksjonspunktDto(Totrinnsvurdering aksjonspunkt,
                                                                                 Behandling behandling,
                                                                                 Optional<Totrinnresultatgrunnlag> totrinnresultatgrunnlag) {
        return new TotrinnskontrollAksjonspunkterDto.Builder()
            .medAksjonspunktKode(aksjonspunkt.getAksjonspunktDefinisjon())
            .medOpptjeningAktiviteter(totrinnskontrollAktivitetDtoTjeneste.hentAktiviterEndretForOpptjening(aksjonspunkt, behandling,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getGrunnlagUuid)))
            .medBeregningDto(totrinnsBeregningDtoTjeneste.hentBeregningDto(aksjonspunkt, behandling,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getBeregningsgrunnlagUuid)))
            .medBesluttersBegrunnelse(aksjonspunkt.getBegrunnelse())
            .medArbeidsforhold(totrinnArbeidsforholdDtoTjeneste.hentArbeidsforhold(behandling, aksjonspunkt,
                totrinnresultatgrunnlag.flatMap(Totrinnresultatgrunnlag::getGrunnlagUuid)))
            .medTotrinnskontrollGodkjent(aksjonspunkt.isGodkjent())
            .medVurderPaNyttArsaker(hentVurderPåNyttÅrsaker(aksjonspunkt))
            .build();
    }

    private Set<TotrinnskontrollVurderÅrsak> hentVurderPåNyttÅrsaker(Totrinnsvurdering aksjonspunkt) {
        return aksjonspunkt.getVurderPåNyttÅrsaker().stream()
            .map(VurderÅrsakTotrinnsvurdering::getÅrsaksType)
            .map(arsakType -> new TotrinnskontrollVurderÅrsak(arsakType))
            .collect(Collectors.toSet());
    }
}
