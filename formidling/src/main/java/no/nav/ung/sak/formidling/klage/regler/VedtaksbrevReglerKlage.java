package no.nav.ung.sak.formidling.klage.regler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageRepository;
import no.nav.ung.sak.behandlingslager.behandling.klage.KlageUtredningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.vedtak.regler.*;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;

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

    public BehandlingVedtaksbrevResultat kjør(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var klageutredning = klageRepository.hentKlageUtredning(behandlingId);
        return bestemResultat(behandling, klageutredning);
    }

    private BehandlingVedtaksbrevResultat bestemResultat(Behandling behandling, KlageUtredningEntitet klageutredning) {
        var klagevurderingFørsteinstans = klageutredning.hentKlageVurderingType(KlageVurdertAv.VEDTAKSINSTANS)
            .orElseThrow(() -> new IllegalStateException("Trenger vurdering av førsteinstans for å kunne vise vedtaksbrev for klage"));

        var strategyResultater = innholdbyggerStrategies.stream()
            .filter(it -> it.skalEvaluere(behandling, klagevurderingFørsteinstans))
            .map(it -> it.evaluer(behandling, klageutredning))
            .toList();

        if (strategyResultater.size() > 1) {
            throw new IllegalStateException("Kan ikke ha flere resultater for strategier ved klage: " + strategyResultater.stream()
                .map(VedtaksbrevStrategyResultat::dokumentMalType)
                .map(DokumentMalType::getNavn)
                .collect(Collectors.joining(", "))
            );
        }

        var ingenBrevResultat = strategyResultater.stream()
            .filter(it -> it.bygger() == null)
            .toList();

        if (!ingenBrevResultat.isEmpty()) {
            return BehandlingVedtaksbrevResultat.utenBrev(
                null,
                ingenBrevResultat.stream()
                .map(it -> VedtaksbrevRegelResultat.ingenBrev(it.ingenBrevÅrsakType(), it.forklaring()))
                .toList());
        }

        var automatiskBrevResultat = strategyResultater.stream()
            .filter(it -> it.bygger() != null)
            .toList();

        if (automatiskBrevResultat.isEmpty()) {
            throw new IllegalStateException("Ingen innholdsbyggere for klagebrevet");
        }

        var automatiskVedtaksbrevResultater = byggAutomatiskVedtaksbrevResultat(automatiskBrevResultat);
        return BehandlingVedtaksbrevResultat.medBrev(null, automatiskVedtaksbrevResultater);
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
