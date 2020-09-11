package no.nav.k9.sak.domene.behandling.steg.vedtak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.hendelse.FinnAnsvarligSaksbehandler;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BehandlingVedtakTjeneste {

    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;

    BehandlingVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakTjeneste(BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                                    BehandlingRepositoryProvider repositoryProvider,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void opprettBehandlingVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        Long behandlingId = behandling.getId();

        VedtakResultatType vedtakResultatType;
        Optional<LocalDate> opphørsdato = Optional.empty();
        Optional<LocalDate> skjæringstidspunkt = Optional.empty();
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        if (behandling.erRevurdering()) {
            opphørsdato = skjæringstidspunktTjeneste.getOpphørsdato(ref);
            skjæringstidspunkt = skjæringstidspunkter.getSkjæringstidspunktHvisUtledet();
        }
        vedtakResultatType = utledVedtakResultatType(behandling, opphørsdato, skjæringstidspunkt);
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

    VedtakResultatType utledVedtakResultatType(Behandling behandling, Optional<LocalDate> opphørsdato, Optional<LocalDate> skjæringstidspunkt) {
        Objects.requireNonNull(behandling, "behandling");
        var behandlingResultatType = behandling.getBehandlingResultatType();

        if (BehandlingResultatType.INNVILGET.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.INNVILGET_ENDRING.equals(behandlingResultatType)) {
            return VedtakResultatType.INNVILGET;
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            var originalBehandlingId = behandling.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Kan ikke ha resultat INGEN ENDRING uten å ha en original behandling"));
            var originalBehandling = this.behandlingRepository.hentBehandling(originalBehandlingId);
            return utledVedtakResultatType(originalBehandling, Optional.empty(), skjæringstidspunkt);
        }
        if (BehandlingResultatType.OPPHØR.equals(behandlingResultatType)) {
            if (opphørsdato.isPresent() && skjæringstidspunkt.isPresent() && opphørsdato.get().isAfter(skjæringstidspunkt.get())) {
                return VedtakResultatType.INNVILGET;
            }
        }
        return VedtakResultatType.AVSLAG;
    }
}
