package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.EndrePerioderGrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Årsak;

@ApplicationScoped
@BehandlingStegRef(kode = "BEKREFT_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class BekreftUttakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BekreftetUttakTjeneste bekreftetUttakTjeneste;
    private UttakTjeneste uttakTjeneste;
    private Boolean enableBekreftUttak;

    BekreftUttakSteg() {
        // CDI
    }

    @Inject
    private BekreftUttakSteg(BehandlingRepository behandlingRepository,
                             BekreftetUttakTjeneste bekreftetUttakTjeneste,
                             UttakTjeneste uttakTjeneste,
                             @KonfigVerdi(value = "psb.enable.bekreft.uttak", defaultVerdi = "false") Boolean enableBekreftUttak) {
        this.behandlingRepository = behandlingRepository;
        this.bekreftetUttakTjeneste = bekreftetUttakTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.enableBekreftUttak = enableBekreftUttak;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var perioderSomHarBlittAvslått = bekreftetUttakTjeneste.utledPerioderTilVurderingSomBlittAvslåttIBeregning(kontekst.getBehandlingId());

        if (!perioderSomHarBlittAvslått.isEmpty() && enableBekreftUttak) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

            var request = new EndrePerioderGrunnlag(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getUuid().toString(), opprettMap(perioderSomHarBlittAvslått));
            uttakTjeneste.endreUttaksplan(request);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Map<LukketPeriode, Årsak> opprettMap(NavigableSet<DatoIntervallEntitet> perioderSomHarBlittAvslått) {
        var map = new HashMap<LukketPeriode, Årsak>();
        for (DatoIntervallEntitet periode : perioderSomHarBlittAvslått) {
            map.put(new LukketPeriode(periode.getFomDato(), periode.getTomDato()), Årsak.FOR_LAV_INNTEKT);
        }
        return map;
    }
}
