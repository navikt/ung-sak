package no.nav.foreldrepenger.skjæringstidspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

import java.time.LocalDate;

@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste foreldrepengerTjeneste;

    SkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(BehandlingRepository behandlingRepository,
                                          @FagsakYtelseTypeRef SkjæringstidspunktTjeneste foreldrepengerTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.foreldrepengerTjeneste = foreldrepengerTjeneste;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erYtelseBehandling()) {
            if (behandling.getFagsakYtelseType().gjelderForeldrepenger()) {
                return foreldrepengerTjeneste.getSkjæringstidspunkter(behandlingId);
            }
            // FIXME K9 Definer skjæringstidspunkt for PSB
            return Skjæringstidspunkt.builder()
                .medFørsteUttaksdato(LocalDate.now().minusDays(10))
                .medSkjæringstidspunktBeregning(LocalDate.now().minusDays(40))
                .medUtledetSkjæringstidspunkt(LocalDate.now().minusDays(40))
                .medSkjæringstidspunktOpptjening(LocalDate.now().minusDays(40))
                .build();
            //throw new IllegalStateException("Ukjent ytelse type." + behandling.getFagsakYtelseType());
        } else {
            // returner tom container for andre behandlingtyper
            // (så ser vi om det evt. er noen call paths som kaller på noen form for skjæringstidspunkt)
            return Skjæringstidspunkt.builder().build();
        }
    }

}
