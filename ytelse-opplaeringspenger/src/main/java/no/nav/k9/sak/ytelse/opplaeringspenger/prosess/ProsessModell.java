package no.nav.k9.sak.ytelse.opplaeringspenger.prosess;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellImpl;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
public class ProsessModell {
    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.OPPLÆRINGSPENGER;

    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.VURDER_UTLAND)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.VURDER_SØKNADSFRIST)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.INREG_AVSL)
            .medSteg(BehandlingStegType.OVERGANG_FRA_INFOTRYGD)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, StartpunktType.KONTROLLER_ARBEIDSFORHOLD)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA, StartpunktType.KONTROLLER_FAKTA)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, StartpunktType.INNGANGSVILKÅR_MEDISINSK)
            .medSteg(BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR)
            .medSteg(BehandlingStegType.POST_VURDER_MEDISINSKVILKÅR)
            .medSteg(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP)
            .medSteg(BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE, StartpunktType.OPPTJENING)
            .medSteg(BehandlingStegType.VURDER_OPPTJENING_FAKTA)
            .medSteg(BehandlingStegType.VURDER_OPPTJENINGSVILKÅR)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA_UTTAK, StartpunktType.UTTAKSVILKÅR, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.PRECONDITION_BEREGNING)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING)
            .medSteg(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING)
            .medSteg(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG_2)
            .medSteg(BehandlingStegType.VURDER_VILKAR_BERGRUNN)
            .medSteg(BehandlingStegType.VURDER_UTTAK_V2, StartpunktType.UTTAKSVILKÅR_VURDERING)
            .medSteg(BehandlingStegType.VURDER_REF_BERGRUNN)
            .medSteg(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.VURDER_MANUELT_BREV)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
    @BehandlingTypeRef(BehandlingType.REVURDERING)
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, YTELSE_TYPE);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.VARSEL_REVURDERING)
            .medSteg(BehandlingStegType.VURDER_UTLAND)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.VURDER_SØKNADSFRIST)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.INREG_AVSL)
            .medSteg(BehandlingStegType.OVERGANG_FRA_INFOTRYGD)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, StartpunktType.KONTROLLER_ARBEIDSFORHOLD)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA, StartpunktType.KONTROLLER_FAKTA)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.VURDER_MEDISINSKE_VILKÅR, StartpunktType.INNGANGSVILKÅR_MEDISINSK)
            .medSteg(BehandlingStegType.VURDER_NØDVENDIGHETS_VILKÅR)
            .medSteg(BehandlingStegType.POST_VURDER_MEDISINSKVILKÅR)
            .medSteg(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP)
            .medSteg(BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE, StartpunktType.OPPTJENING)
            .medSteg(BehandlingStegType.VURDER_OPPTJENING_FAKTA)
            .medSteg(BehandlingStegType.VURDER_OPPTJENINGSVILKÅR)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA_UTTAK, StartpunktType.UTTAKSVILKÅR, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.PRECONDITION_BEREGNING)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET_BEREGNING)
            .medSteg(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING)
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING)
            .medSteg(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG_2)
            .medSteg(BehandlingStegType.VURDER_VILKAR_BERGRUNN)
            .medSteg(BehandlingStegType.VURDER_UTTAK_V2, StartpunktType.UTTAKSVILKÅR_VURDERING)
            .medSteg(BehandlingStegType.VURDER_REF_BERGRUNN)
            .medSteg(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.VURDER_TILBAKETREKK)
            .medSteg(BehandlingStegType.HINDRE_TILBAKETREKK)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.VURDER_MANUELT_BREV)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
