package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

import java.util.List;
import java.util.Set;

@Dependent
public class RegisterinnhentingHistorikkinnslagTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;

    RegisterinnhentingHistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RegisterinnhentingHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public void opprettHistorikkinnslagForNyeRegisteropplysninger(Behandling behandling) {
        var nyeRegisteropplysningerInnslagBuilder = new Historikkinnslag.Builder();
        nyeRegisteropplysningerInnslagBuilder.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslagBuilder.medTittel("Nye registeropplysninger");
        nyeRegisteropplysningerInnslagBuilder.medBehandlingId(behandling.getId());
        nyeRegisteropplysningerInnslagBuilder.medFagsakId(behandling.getFagsakId());
        nyeRegisteropplysningerInnslagBuilder.addLinje("Saksbehandling starter på nytt");

        historikkinnslagRepository.lagre(nyeRegisteropplysningerInnslagBuilder.build());
    }

    public void opprettHistorikkinnslagForTilbakespoling(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg) {
        var nyeRegisteropplysningerInnslagBuilder = new Historikkinnslag.Builder();
        nyeRegisteropplysningerInnslagBuilder.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslagBuilder.medTittel("Behandlingen er flyttet");
        nyeRegisteropplysningerInnslagBuilder.medBehandlingId(behandling.getId());
        nyeRegisteropplysningerInnslagBuilder.medFagsakId(behandling.getFagsakId());
        historikkinnslagRepository.lagre(nyeRegisteropplysningerInnslagBuilder.build());
    }

    public void opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(Behandling behandling, Set<BehandlingÅrsakType> behandlingÅrsakType) {
        var nyeRegisteropplysningerInnslagBuilder = new Historikkinnslag.Builder();
        nyeRegisteropplysningerInnslagBuilder.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslagBuilder.medTittel("Behandlingen oppdatert med nye opplysninger");
        nyeRegisteropplysningerInnslagBuilder.medBehandlingId(behandling.getId());
        nyeRegisteropplysningerInnslagBuilder.medFagsakId(behandling.getFagsakId());
        behandlingÅrsakType.stream().map(BehandlingÅrsakType::getNavn).forEach(nyeRegisteropplysningerInnslagBuilder::addLinje);
        historikkinnslagRepository.lagre(nyeRegisteropplysningerInnslagBuilder.build());
    }
}
