package no.nav.k9.sak.historikk;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagTotrinnsvurdering;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;

public class HistorikkInnslagTekstBuilderTest {

    @Test
    public void testHistorikkinnslagTekstSakRetur() {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.SAK_RETUR);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
        Map<SkjermlenkeType, List<HistorikkinnslagTotrinnsvurdering>> vurdering = new HashMap<>();

        List<HistorikkinnslagTotrinnsvurdering> vurderingUtenVilkar = new ArrayList<>();

        HistorikkinnslagTotrinnsvurdering vurderPåNytt = new HistorikkinnslagTotrinnsvurdering();
        vurderPåNytt.setGodkjent(false);
        vurderPåNytt.setBegrunnelse("Må vurderes igjen. Se på dokumentasjon.");
        vurderPåNytt.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE);
        vurderPåNytt.setAksjonspunktSistEndret(LocalDateTime.now());
        vurdering.put(SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP, Collections.singletonList(vurderPåNytt));

        HistorikkinnslagTotrinnsvurdering godkjent = new HistorikkinnslagTotrinnsvurdering();
        godkjent.setGodkjent(true);
        godkjent.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
        godkjent.setAksjonspunktSistEndret(LocalDateTime.now());

        HistorikkinnslagTotrinnsvurdering vurderPåNytt2 = new HistorikkinnslagTotrinnsvurdering();
        vurderPåNytt2.setGodkjent(false);
        vurderPåNytt2.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
        vurderPåNytt2.setBegrunnelse("Ikke enig.");
        vurderPåNytt2.setAksjonspunktSistEndret(LocalDateTime.now());
        vurdering.put(SkjermlenkeType.FAKTA_FOR_OPPTJENING, Arrays.asList(godkjent, vurderPåNytt2));

        HistorikkinnslagTotrinnsvurdering vurderPåNytt3 = new HistorikkinnslagTotrinnsvurdering();
        vurderPåNytt3.setGodkjent(false);
        vurderPåNytt3.setAksjonspunktDefinisjon(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
        vurderPåNytt3.setBegrunnelse("Ikke enig.");
        vurderPåNytt3.setAksjonspunktSistEndret(LocalDateTime.now());

        vurderingUtenVilkar.add(vurderPåNytt3);

        List<HistorikkinnslagDel> deler = tekstBuilder
            .medTotrinnsvurdering(vurdering, vurderingUtenVilkar)
            .medHendelse(HistorikkinnslagType.SAK_RETUR)
            .build(historikkinnslag);

        assertThat(deler).hasSize(3);
        HistorikkinnslagDel historikkinnslagDel = deler.get(0);
        List<HistorikkinnslagTotrinnsvurdering> aksjonspunkter = historikkinnslagDel.getTotrinnsvurderinger();
        assertThat(aksjonspunkter).hasSize(1);
        HistorikkinnslagTotrinnsvurdering aksjonspunkt = aksjonspunkter.get(0);
        assertThat(aksjonspunkt.getAksjonspunktDefinisjon()).as("aksjonspunktKode").isEqualTo(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
        assertThat(aksjonspunkt.erGodkjent()).as("godkjent").isFalse();
        assertThat(aksjonspunkt.getBegrunnelse()).as("begrunnelse").isEqualTo("Ikke enig.");
    }

}
