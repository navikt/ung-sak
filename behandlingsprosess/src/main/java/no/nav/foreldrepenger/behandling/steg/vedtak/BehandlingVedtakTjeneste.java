package no.nav.foreldrepenger.behandling.steg.vedtak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.hendelse.FinnAnsvarligSaksbehandler;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;

@ApplicationScoped
public class BehandlingVedtakTjeneste {

    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    BehandlingVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakTjeneste(BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                                    BehandlingRepositoryProvider repositoryProvider, OpphørUttakTjeneste opphørUttakTjeneste,
                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.opphørUttakTjeneste = opphørUttakTjeneste;
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
            opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref);
            skjæringstidspunkt = skjæringstidspunkter.getSkjæringstidspunktHvisUtledet();
        }
        vedtakResultatType = UtledVedtakResultatType.utled(behandling, behandling.getBehandlingResultatType(), opphørsdato, skjæringstidspunkt);
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
}
