package no.nav.ung.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.vedtak.OppdaterAnsvarligSaksbehandlerTjeneste;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.NavKontorForeslåVilkårAksjonspunktDto;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;

import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = NavKontorForeslåVilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class NavKontorForeslårVilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<NavKontorForeslåVilkårAksjonspunktDto> {

    private Instance<OppdaterAnsvarligSaksbehandlerTjeneste> oppdaterAnsvarligSaksbehandlerTjenester;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag;

    NavKontorForeslårVilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public NavKontorForeslårVilkårAksjonspunktOppdaterer(OpprettToTrinnsgrunnlag opprettToTrinnsgrunnlag,
                                                         HistorikkinnslagRepository historikkinnslagRepository,
                                                         BehandlingRepository behandlingRepository,
                                                         @Any Instance<OppdaterAnsvarligSaksbehandlerTjeneste> oppdaterAnsvarligSaksbehandlerTjenester) {
        this.opprettToTrinnsgrunnlag = opprettToTrinnsgrunnlag;
        this.oppdaterAnsvarligSaksbehandlerTjenester = oppdaterAnsvarligSaksbehandlerTjenester;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(NavKontorForeslåVilkårAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        OppdaterAnsvarligSaksbehandlerTjeneste oppdaterAnsvarligSaksbehandlerTjeneste = OppdaterAnsvarligSaksbehandlerTjeneste.finnTjeneste(oppdaterAnsvarligSaksbehandlerTjenester, param.getRef().getFagsakYtelseType());
        oppdaterAnsvarligSaksbehandlerTjeneste.oppdaterAnsvarligSaksbehandler(Set.of(dto), param.getBehandlingId());

        OppdateringResultat.Builder builder = OppdateringResultat.builder();

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());

        opprettToTrinnsgrunnlag.settNyttTotrinnsgrunnlag(behandling);

        opprettHistorikkinnslag(behandling);

        return builder.build();
    }

    private void opprettHistorikkinnslag(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.NAV_KONTOR_SAKSBEHANDLER)
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel(SkjermlenkeType.NAV_KONTOR_FORESLÅR_VILKÅR)
            .addLinje("Vilkårsvurderinger utført og sendt til beslutter")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

}
