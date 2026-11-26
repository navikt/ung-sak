package no.nav.ung.ytelse.aktivitetspenger.prosess;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellImpl;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
public class ProsessModell {

    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling(KontrollbehandlingBryter kontrollbehandlingBryter) {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, FagsakYtelseType.AKTIVITETSPENGER);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG, StartpunktType.START)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_SØKNADSFRIST)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.VURDER_BOSTEDVILKÅR)
            .medSteg(BehandlingStegType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .medSteg(BehandlingStegType.VURDER_BISTANDSVILKÅR)
            .medSteg(BehandlingStegType.VURDER_AKTIVITETSVILKÅR)
            .medSteg(BehandlingStegType.NAV_KONTOR_FORESLÅ_VILKÅR)
            .medSteg(BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR)
            .medSteg(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.AKTIVITETSPENGER_BEREGNING, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET, StartpunktType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        medVentepunktForVedtaksprioritetHvisBryterErPå(modellBuilder, kontrollbehandlingBryter);
        modellBuilder
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }


    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    @BehandlingTypeRef(BehandlingType.REVURDERING)
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering(KontrollbehandlingBryter kontrollbehandlingBryter) {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, FagsakYtelseType.AKTIVITETSPENGER);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG, StartpunktType.START)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_SØKNADSFRIST)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.VURDER_FAKTA_OM_BOSTED)
            .medSteg(BehandlingStegType.VURDER_BOSTEDVILKÅR)
            .medSteg(BehandlingStegType.VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER)
            .medSteg(BehandlingStegType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .medSteg(BehandlingStegType.VURDER_FAKTA_OM_BISTAND)
            .medSteg(BehandlingStegType.VURDER_BISTANDSVILKÅR)
            .medSteg(BehandlingStegType.VURDER_FAKTA_OM_AKTIVITET)
            .medSteg(BehandlingStegType.VURDER_AKTIVITETSVILKÅR)
            .medSteg(BehandlingStegType.NAV_KONTOR_FORESLÅ_VILKÅR)
            .medSteg(BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR)
            .medSteg(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.AKTIVITETSPENGER_BEREGNING, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET, StartpunktType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        medVentepunktForVedtaksprioritetHvisBryterErPå(modellBuilder, kontrollbehandlingBryter);
        modellBuilder
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    /**
     * Kontrollbehandlingen går bare gjennom stegene rundt inntektskontrollen. Vilkår, perioder og ytelsesgrunnlag
     * kommer fra kopi og synkronisering av vedtatte data, så alle endringer starter i kompletthetssteget.
     */
    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    @BehandlingTypeRef(BehandlingType.KONTROLL)
    @Produces
    @ApplicationScoped
    public BehandlingModell kontroll() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.KONTROLL, FagsakYtelseType.AKTIVITETSPENGER);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET, StartpunktType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.KONTROLLER_REGISTER_INNTEKT)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.VURDER_PARALLELLE_BEHANDLINGER)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.SIMULER_OPPDRAG)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    /**
     * Ventepunktet for vedtaksprioritet står i modellene for førstegangsbehandling og revurdering bare når bryteren er
     * på. Ellers lagres verken stegkoden eller autopunktet.
     */
    private static void medVentepunktForVedtaksprioritetHvisBryterErPå(BehandlingModellImpl.BehandlingModellBuilder modellBuilder, KontrollbehandlingBryter kontrollbehandlingBryter) {
        if (kontrollbehandlingBryter.erPå(FagsakYtelseType.AKTIVITETSPENGER)) {
            modellBuilder.medSteg(BehandlingStegType.VURDER_PARALLELLE_BEHANDLINGER);
        }
    }

}
