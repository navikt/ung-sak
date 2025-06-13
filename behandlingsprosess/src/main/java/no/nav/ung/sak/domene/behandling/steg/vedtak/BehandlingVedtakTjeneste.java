package no.nav.ung.sak.domene.behandling.steg.vedtak;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.hendelse.FinnAnsvarligSaksbehandler;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.domene.vedtak.impl.BehandlingVedtakEventPubliserer;

@ApplicationScoped
public class BehandlingVedtakTjeneste {

    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingRepository behandlingRepository;

    BehandlingVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakTjeneste(BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                                    BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    public void opprettBehandlingVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        Long behandlingId = behandling.getId();

        VedtakResultatType vedtakResultatType;
        var ref = BehandlingReferanse.fra(behandling);
        vedtakResultatType = utledVedtakResultatType(behandling);
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);
        LocalDateTime vedtakstidspunkt = LocalDateTime.now();

        boolean erRevurderingMedUendretUtfall = ref.getBehandlingResultat().isBehandlingsresultatIkkeEndret();

        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder(behandlingId)
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler(ansvarligSaksbehandler)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medBeslutning(erRevurderingMedUendretUtfall)
            .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, kontekst.getSkriveLås());
        behandlingVedtakEventPubliserer.fireEvent(behandlingVedtak, behandling);
    }

    VedtakResultatType utledVedtakResultatType(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
        var behandlingResultatType = behandling.getBehandlingResultatType();

        if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.DELVIS_INNVILGET.equals(behandlingResultatType)){
            return VedtakResultatType.DELVIS_INNVILGET;
        }
        if (BehandlingResultatType.INNVILGET_ENDRING.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            var originalBehandlingId = behandling.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Kan ikke ha resultat INGEN ENDRING uten å ha en original behandling"));
            var originalBehandling = this.behandlingRepository.hentBehandling(originalBehandlingId);
            return utledVedtakResultatType(originalBehandling);
        }
        return VedtakResultatType.AVSLAG;
    }
}
