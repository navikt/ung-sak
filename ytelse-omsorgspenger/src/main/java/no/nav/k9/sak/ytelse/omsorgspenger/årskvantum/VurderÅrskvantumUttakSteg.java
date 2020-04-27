package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.aarskvantum.kontrakter.Uttaksplan;
import no.nav.k9.aarskvantum.kontrakter.Årsak;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
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

        if (skalDetLagesAksjonspunkt(årskvantumResultat.getUttaksplan())) {
            try {
                log.info("Setter behandling på vent etter følgende respons fra årskvantum" +
                    "\nrespons='{}'", JsonObjectMapper.getJson(årskvantumResultat));
            } catch (IOException e) {
                log.info("Feilet i serialisering av årskvantum respons: " + årskvantumResultat);
            }

            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(opprettAksjonspunktForÅrskvantum().getAksjonspunktDefinisjon()));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
            //TODO 2 kan vi innvilge deler av periodene og avslå resten?
        }

        // K9 TODO:
        // 1. kalle årskvantum for å få vurdet fraværet
        //    a. hvis Ok gå videre uten aksjonspunkter
        //    b. hvis Ikke Ok opprett aksjonspunkt som må løses i dette steget (fosterforeldre, delt bosted, etc.)
        // 2. Lag REST tjeneste for GUI - vise hvor mye brukt (basert på samme tjeneste som kalles her
        // 3. Lag AksjonspunktOppdaterer for å skrive ned oppdatert kvantum til Årskvantum og la steget kjøre på nytt.

    }

    private AksjonspunktResultat opprettAksjonspunktForÅrskvantum() {
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.VURDER_ÅRSKVANTUM_KVOTE;
        return AksjonspunktResultat.opprettForAksjonspunkt(apDef);
    }


    public boolean skalDetLagesAksjonspunkt(Uttaksplan uttaksplanOmsorgspenger) {
        for (Aktivitet uttaksPlanOmsorgspengerAktivitet : uttaksplanOmsorgspenger.getAktiviteter()) {
            for (Uttaksperiode uttaksperiodeOmsorgspenger : uttaksPlanOmsorgspengerAktivitet.getUttaksperioder()) {
                if (Årsak.AVSLÅTT_IKKE_FLERE_DAGER.equals(uttaksperiodeOmsorgspenger.getårsak())
                    || Årsak.AVSLÅTT_UIDENTIFISERT_RAMMEVEDTAK.equals(uttaksperiodeOmsorgspenger.getårsak())
                    || Årsak.AVSLÅTT_KREVER_LEGEERKLÆRING.equals(uttaksperiodeOmsorgspenger.getårsak())) {
                    return true;
                }
            }
        }

        return false;
    }
}
