package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt.Builder;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste, SkjæringstidspunktRegisterinnhentingTjeneste {

    private BehandlingRepository behandlingRepository;
    private OpptjeningRepository opptjeningRepository;
    private FordelingRepository fordelingRepository;

    DefaultSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public DefaultSkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository, OpptjeningRepository opptjeningRepository, FordelingRepository fordelingRepository) {
        this.behandlingRepository = behandlingRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.fordelingRepository = fordelingRepository;
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        // FIXME K9 skjæringstidspunkt
        return førsteUttaksdag(behandlingId);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medFørsteUttaksdato(førsteUttaksdato);
        builder.medUtledetSkjæringstidspunkt(førsteUttaksdato);

        opptjeningRepository.finnOpptjening(behandlingId)
            .map(opptjening -> opptjening.getTom().plusDays(1))
            .ifPresent(skjæringstidspunkt -> {
                builder.medSkjæringstidspunktOpptjening(skjæringstidspunkt);
                builder.medUtledetSkjæringstidspunkt(skjæringstidspunkt);
            });

        return builder.build();
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        final var fordeling = fordelingRepository.hentHvisEksisterer(behandlingId);

        if(fordeling.isPresent()) {
            final var oppgittFordeling = fordeling.get();
            return oppgittFordeling.getPerioder()
                .stream()
                .map(FordelingPeriode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        }
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return behandling.getOpprettetDato().toLocalDate();
    }
}
