package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.historikk.HistorikkBegrunnelseType;
import no.nav.ung.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.historikk.HistorikkInnslagTekstBuilder;

@Dependent
public class RegisterinnhentingHistorikkinnslagTjeneste {

    private HistorikkRepository historikkRepository;

    RegisterinnhentingHistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public RegisterinnhentingHistorikkinnslagTjeneste(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslagForNyeRegisteropplysninger(Behandling behandling) {
        Historikkinnslag nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.NYE_REGOPPLYSNINGER);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.NYE_REGOPPLYSNINGER)
            .medBegrunnelse(HistorikkBegrunnelseType.SAKSBEH_START_PA_NYTT);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    public void opprettHistorikkinnslagForTilbakespoling(Behandling behandling, BehandlingStegType førSteg, BehandlingStegType etterSteg) {
        Historikkinnslag nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
            .medBegrunnelse("Behandlingen er flyttet fra " + førSteg.getNavn() + " tilbake til " + etterSteg.getNavn());
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    public void opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Historikkinnslag nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());

        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(behandlingÅrsakType);
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }
}
