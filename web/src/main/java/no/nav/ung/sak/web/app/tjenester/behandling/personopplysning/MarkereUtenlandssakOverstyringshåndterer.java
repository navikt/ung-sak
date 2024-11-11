package no.nav.ung.sak.web.app.tjenester.behandling.personopplysning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltVerdiType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.ung.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.ung.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.ung.sak.kontrakt.person.OverstyringUtenlandssakMarkeringDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringUtenlandssakMarkeringDto.class, adapter = Overstyringshåndterer.class)
public class MarkereUtenlandssakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringUtenlandssakMarkeringDto> {

    private HistorikkTjenesteAdapter historikkAdapter;

    MarkereUtenlandssakOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MarkereUtenlandssakOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter) {
        super(historikkAdapter, AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE);
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringUtenlandssakMarkeringDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        OppdateringResultat.Builder builder = OppdateringResultat.builder();
        behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK)
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringUtenlandssakMarkeringDto dto) {
        HistorikkEndretFeltVerdiType fraVerdi = getHistorikkEndretFeltVerdiTypeFra(dto.getGammelVerdi());
        HistorikkEndretFeltVerdiType tilVerdi = getHistorikkEndretFeltVerdiTypeFra(dto.getBegrunnelse());

        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);

        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.FAKTA_ENDRET)
            .medSkjermlenke(SkjermlenkeType.UTLAND)
            .medEndretFelt(HistorikkEndretFeltType.UTLAND, fraVerdi, tilVerdi);

        builder.build(historikkinnslag);
        historikkAdapter.lagInnslag(historikkinnslag);
    }

    private HistorikkEndretFeltVerdiType getHistorikkEndretFeltVerdiTypeFra(String feltVerdi) {
        HistorikkEndretFeltVerdiType nasjonal = HistorikkEndretFeltVerdiType.NASJONAL;
        HistorikkEndretFeltVerdiType eøs = HistorikkEndretFeltVerdiType.EØS_BOSATT_NORGE;
        HistorikkEndretFeltVerdiType bosattUtland = HistorikkEndretFeltVerdiType.BOSATT_UTLAND;

        return eøs.getKode().equals(feltVerdi) ? eøs
            : bosattUtland.getKode().equals(feltVerdi) ? bosattUtland
                : nasjonal;
    }
}
