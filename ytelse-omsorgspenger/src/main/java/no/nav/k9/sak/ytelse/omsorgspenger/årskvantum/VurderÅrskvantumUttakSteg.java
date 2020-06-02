package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Bekreftet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.Vilkår;
import no.nav.k9.aarskvantum.kontrakter.ÅrskvantumResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@ApplicationScoped
@BehandlingStegRef(kode = "VURDER_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class VurderÅrskvantumUttakSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(VurderÅrskvantumUttakSteg.class);

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste stpTjeneste;
    private ÅrskvantumTjeneste årskvantumTjeneste;


    VurderÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public VurderÅrskvantumUttakSteg(BehandlingRepository behandlingRepository,
                                     SkjæringstidspunktTjeneste stpTjeneste,
                                     ÅrskvantumTjeneste årskvantumTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.stpTjeneste = stpTjeneste;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var stp = stpTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, stp);


        var årskvantumResultat = årskvantumTjeneste.hentÅrskvantumUttak(ref);

        if (skalDetLagesAksjonspunkt(årskvantumResultat)) {
            try {
                log.debug("Setter behandling på vent etter følgende respons fra årskvantum" +
                    "\nrespons='{}'", JsonObjectMapper.getJson(årskvantumResultat));
            } catch (IOException e) {
                log.info("Feilet i serialisering av årskvantum respons: " + årskvantumResultat);
            }

            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(opprettAksjonspunktForÅrskvantum().getAksjonspunktDefinisjon()));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        if (!førsteSteg.equals(sisteSteg)) {
            årskvantumTjeneste.slettUttaksplan(kontekst.getBehandlingId());
        }
    }


    private AksjonspunktResultat opprettAksjonspunktForÅrskvantum() {
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.VURDER_ÅRSKVANTUM_KVOTE;
        return AksjonspunktResultat.opprettForAksjonspunkt(apDef);
    }


    public boolean skalDetLagesAksjonspunkt(ÅrskvantumResultat årskvantumResultat) {
        if (!Bekreftet.MANUELTBEKREFTET.equals(årskvantumResultat.getUttaksplan().getBekreftet())) {
            for (Aktivitet uttaksPlanOmsorgspengerAktivitet : årskvantumResultat.getUttaksplan().getAktiviteter()) {
                for (Uttaksperiode uttaksperiodeOmsorgspenger : uttaksPlanOmsorgspengerAktivitet.getUttaksperioder()) {
                    for (Vilkår vilkår : uttaksperiodeOmsorgspenger.getVurderteVilkår().getVilkår().keySet()) {
                        if ((Vilkår.UIDENTIFISERT_RAMMEVEDTAK.equals(vilkår) || Vilkår.SMITTEVERN.equals(vilkår) || Vilkår.NOK_DAGER.equals(vilkår)) &&
                            (uttaksperiodeOmsorgspenger.getVurderteVilkår().getVilkår().getOrDefault(vilkår, Utfall.INNVILGET).equals(Utfall.AVSLÅTT) ||
                                uttaksperiodeOmsorgspenger.getVurderteVilkår().getVilkår().getOrDefault(vilkår, Utfall.INNVILGET).equals(Utfall.UAVKLART))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
