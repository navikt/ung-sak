package no.nav.ung.sak.behandling.revurdering;

import java.util.List;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

public interface GrunnlagKopierer {
    List<AksjonspunktDefinisjon> AKSJONSPUNKTER_MANUELL_REVURDERING = List.of(AksjonspunktDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING);

    void kopierGrunnlagVedManuellOpprettelse(Behandling original, Behandling ny);

    void kopierGrunnlagVedAutomatiskOpprettelse(Behandling original, Behandling ny);

    default List<AksjonspunktDefinisjon> getApForManuellRevurdering() {
        return AKSJONSPUNKTER_MANUELL_REVURDERING;
    }
}
