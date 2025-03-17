package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.uttalelse.RegisterinntektUttalelseTjeneste;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
import static no.nav.ung.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

@ApplicationScoped
@BehandlingStegRef(value = KONTROLLER_REGISTER_INNTEKT)
@BehandlingTypeRef
@FagsakYtelseTypeRef(UNGDOMSYTELSE)
public class KontrollerInntektSteg implements BehandlingSteg {

    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private RapportertInntektMapper rapportertInntektMapper;
    private KontrollerInntektTjeneste kontrollerInntektTjeneste;
    private RegisterinntektUttalelseTjeneste registerinntektUttalelseTjeneste;


    @Inject
    public KontrollerInntektSteg(ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder,
                                 RapportertInntektMapper rapportertInntektMapper,
                                 KontrollerInntektTjeneste kontrollerInntektTjeneste,
                                 RegisterinntektUttalelseTjeneste registerinntektUttalelseTjeneste) {
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.kontrollerInntektTjeneste = kontrollerInntektTjeneste;
        this.registerinntektUttalelseTjeneste = registerinntektUttalelseTjeneste;
    }

    public KontrollerInntektSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        final var rapporterteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(kontekst.getBehandlingId());
        final var prosessTriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(kontekst.getBehandlingId());
        final var uttalelser = registerinntektUttalelseTjeneste.hentUttalelser(kontekst.getBehandlingId());
        final var registerinntekterForIkkeGodkjentUttalelse = rapportertInntektMapper.finnRegisterinntekterForUttalelse(kontekst.getBehandlingId(), uttalelser);


        final var kontrollResultat = kontrollerInntektTjeneste.utførKontroll(prosessTriggerTidslinje, rapporterteInntekterTidslinje, registerinntekterForIkkeGodkjentUttalelse);

        return BehandleStegResultat.utførtUtenAksjonspunkter(
        );
    }

}
