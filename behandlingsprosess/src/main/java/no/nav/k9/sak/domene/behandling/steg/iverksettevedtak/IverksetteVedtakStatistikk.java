package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.ArrayList;
import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;

@Dependent
public class IverksetteVedtakStatistikk {
    private SensuKlient sensuKlient;
    private VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public IverksetteVedtakStatistikk(SensuKlient sensuKlient, VilkårResultatRepository vilkårResultatRepository) {
        this.sensuKlient = sensuKlient;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public void logMetrikker(Behandling behandling) {
        var fagsak = behandling.getFagsak();
        Long behandlingId = behandling.getId();
        var resultatType = behandling.getBehandlingResultatType();

        var events = new ArrayList<SensuEvent>();

        events.add(SensuEvent.createSensuEvent(
            "steg.iverksetteVedtak",
            Map.of(
                "ytelse_type", fagsak.getYtelseType().getKode(),
                "behandling_type", behandling.getType().getKode(),
                "behandling_resultat", resultatType.getKode()),
            Map.of(
                "antall", 1,
                "antall_manuell", (behandling.getAnsvarligSaksbehandler() != null ? 1 : 0),
                "antall_automatisk", (behandling.getAnsvarligSaksbehandler() != null ? 0 : 1),
                "antall_totrinn", (behandling.isToTrinnsBehandling() ? 1 : 0))));

        if (resultatType.isBehandlingsresultatAvslått()) {
            events.addAll(lagAvslagEvents(behandling, fagsak, behandlingId, resultatType));
        } else if (BehandlingResultatType.INNVILGET.equals(resultatType)) {
            // TODO generer noe statistikk for innvilgede?
        }

        sensuKlient.logMetrics(events);
    }

    private ArrayList<SensuEvent> lagAvslagEvents(Behandling behandling, Fagsak fagsak, Long behandlingId, BehandlingResultatType resultatType) {
        var avslagEvents = new ArrayList<SensuEvent>();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        for (var v : vilkårene.getVilkårMedAvslagsårsaker().entrySet()) {
            for (var a : v.getValue()) {
                avslagEvents.add(SensuEvent.createSensuEvent(
                    "steg.iverksetteVedtak.avslag",
                    Map.of(
                        "ytelse_type", fagsak.getYtelseType().getKode(),
                        "behandling_type", behandling.getType().getKode(),
                        "behandling_resultat", resultatType.getKode(),
                        "behandling_uuid", behandling.getUuid().toString(),
                        "vilkar_type", v.getKey().getKode(),
                        "avslag_arsak", a.getKode()),
                    Map.of("antall", 1)));
            }
        }
        return avslagEvents;
    }
}
