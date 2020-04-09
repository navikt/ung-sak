package no.nav.k9.sak.ytelse.frisinn.skjæringstidspunkt;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.Skjæringstidspunkt.Builder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnSkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

    private UttakRepository uttakRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;

    FrisinnSkjæringstidspunktTjenesteImpl() {
        // CDI
    }

    @Inject
    public FrisinnSkjæringstidspunktTjenesteImpl(UttakRepository uttakRepository,
                                                 UttakTjeneste uttakTjeneste) {
        this.uttakRepository = uttakRepository;
        this.opphørUttakTjeneste = new OpphørUttakTjeneste(uttakTjeneste);
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId, FagsakYtelseType ytelseType) {
        // FIXME K9 skjæringstidspunkt
        return førsteUttaksdag(behandlingId);
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        Builder builder = Skjæringstidspunkt.builder();

        LocalDate førsteUttaksdato = førsteUttaksdag(behandlingId);
        builder.medFørsteUttaksdato(førsteUttaksdato);
        builder.medUtledetSkjæringstidspunkt(LocalDate.of(2020, 03, 01));
        return builder.build();
    }

    @Override
    public Optional<LocalDate> getOpphørsdato(BehandlingReferanse ref) {
        return null;
    }

    private LocalDate førsteUttaksdag(Long behandlingId) {
        var søknadsperioder = uttakRepository.hentOppgittSøknadsperioderHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Mangler sønadsperiode for behandlingId=" + behandlingId));

        return søknadsperioder.getMaksPeriode().getFomDato();
    }

    @Override
    public boolean harAvslåttPeriode(UUID behandlingUuid) {
        return opphørUttakTjeneste.harAvslåttUttakPeriode(behandlingUuid);
    }
}
