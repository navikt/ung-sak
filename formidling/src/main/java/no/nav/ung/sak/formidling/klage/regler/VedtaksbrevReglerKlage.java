package no.nav.ung.sak.formidling.klage.regler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.klage.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.regler.Vedtaksbrev;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegel;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef(BehandlingType.KLAGE)
public class VedtaksbrevReglerKlage implements VedtaksbrevRegel {

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private Instance<VedtaksbrevKlageInnholdbyggerStrategy> innholdbyggerStrategies;

    @Inject
    public VedtaksbrevReglerKlage(
        BehandlingRepository behandlingRepository,
        KlageRepository klageRepository,
        @Any Instance<VedtaksbrevKlageInnholdbyggerStrategy> innholdbyggerStrategies) {
        this.behandlingRepository = behandlingRepository;
        this.klageRepository = klageRepository;
        this.innholdbyggerStrategies = innholdbyggerStrategies;
    }

    public VedtaksbrevReglerKlage() {
    }

    public BehandlingVedtaksbrevResultatKlage kjør(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var klageutredning = klageRepository.hentKlageUtredning(behandlingId);
        return bestemResultat(behandling, klageutredning);
    }

    private BehandlingVedtaksbrevResultatKlage bestemResultat(Behandling behandling, KlageUtredningEntitet klageutredning) {
        var strategyResultater = innholdbyggerStrategies.stream()
            .filter(it -> it.skalEvaluere(behandling, klageutredning.hentGjeldendeKlagevurderingType()))
            .map(it -> it.evaluer(behandling, klageutredning))
            .toList();

        if (strategyResultater.size() > 1) {
            throw new IllegalStateException("Kan ikke ha flere resultater for strategier ved klage: " + strategyResultater.stream()
                .map(VedtaksbrevStrategyResultat::dokumentMalType)
                .map(DokumentMalType::getNavn)
                .collect(Collectors.joining(", "))
            );
        }

        var automatiskBrevResultat = strategyResultater.stream()
            .filter(it -> it.bygger() != null)
            .toList();

        if (automatiskBrevResultat.isEmpty()) {
            throw new IllegalStateException("Ingen innholdsbyggere for klagebrevet");
        }

        var automatiskVedtaksbrevResultater = byggAutomatiskVedtaksbrevResultat(automatiskBrevResultat);
        return BehandlingVedtaksbrevResultatKlage.medBrev(automatiskVedtaksbrevResultater);
    }

    private static List<Vedtaksbrev> byggAutomatiskVedtaksbrevResultat(List<VedtaksbrevStrategyResultat> resultat) {
        return resultat.stream()
            .map(it -> {
                    return new Vedtaksbrev(
                        it.dokumentMalType(),
                        it.bygger(),
                        VedtaksbrevEgenskaper.kanRedigere(true),    // Ikke i bruk i klage
                        null
                    );
                }
            ).toList();
    }
}
