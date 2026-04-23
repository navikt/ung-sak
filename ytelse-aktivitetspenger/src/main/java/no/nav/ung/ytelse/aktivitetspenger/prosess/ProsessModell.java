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
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
public class ProsessModell {

    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    @BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
    @Produces
    @ApplicationScoped
    public BehandlingModell aktivitetspengerDel1() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, FagsakYtelseType.AKTIVITETSPENGER);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG, StartpunktType.START)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_SØKNADSFRIST)
            .medSteg(BehandlingStegType.ALDERSVILKÅRET)
            .medSteg(BehandlingStegType.VURDER_BOSTED)
            .medSteg(BehandlingStegType.VURDER_ANDRE_LIVSOPPHOLDSYTELSER)
            .medSteg(BehandlingStegType.VURDER_BISTANDSVILKÅR)
            .medSteg(BehandlingStegType.LOKALKONTOR_FORESLÅ_VILKÅR)
            .medSteg(BehandlingStegType.LOKALKONTOR_BESLUTTER_VILKÅR)
            .medSteg(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.AKTIVITETSPENGER_BEREGNING, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET, StartpunktType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.KONTROLLER_REGISTER_INNTEKT)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }


    @FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
    @BehandlingTypeRef(BehandlingType.REVURDERING)
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, FagsakYtelseType.AKTIVITETSPENGER);
        modellBuilder
            .medSteg(BehandlingStegType.START_STEG, StartpunktType.START)
            .medSteg(BehandlingStegType.INIT_PERIODER, StartpunktType.INIT_PERIODER)
            .medSteg(BehandlingStegType.INIT_VILKÅR)
            .medSteg(BehandlingStegType.INNHENT_REGISTEROPP)
            .medSteg(BehandlingStegType.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
            .medSteg(BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT)
            .medSteg(BehandlingStegType.AKTIVITETSPENGER_BEREGNING, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.VURDER_KOMPLETTHET, StartpunktType.VURDER_KOMPLETTHET)
            .medSteg(BehandlingStegType.KONTROLLER_REGISTER_INNTEKT)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.FORESLÅ_VEDTAK)
            .medSteg(BehandlingStegType.FATTE_VEDTAK)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
