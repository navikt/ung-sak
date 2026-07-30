package no.nav.ung.ytelse.ungdomsprogramytelsen.perioder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsytelseKvalifiserteYtelsesperioderTjeneste implements KvalifiserteYtelsesperioderTjeneste {

    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    @Inject
    public UngdomsytelseKvalifiserteYtelsesperioderTjeneste(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    public UngdomsytelseKvalifiserteYtelsesperioderTjeneste() {
        // CDI
    }

    @Override
    public LocalDateTimeline<Boolean> finnPeriodeTidslinje(Long behandlingId) {
        return ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId).disjoint(finnAvslåttUttakTidslinje(behandlingId));
    }

    @Override
    public LocalDateTimeline<Boolean> finnInitiellPeriodeTidslinje(Long behandlingId) {
        return ungdomsprogramPeriodeTjeneste.finnInitiellPeriodeTidslinje(behandlingId).disjoint(finnAvslåttUttakTidslinje(behandlingId));
    }

    private LocalDateTimeline<Boolean> finnAvslåttUttakTidslinje(Long behandlingId) {
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId);
        if (ungdomsytelseGrunnlag.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        return ungdomsytelseGrunnlag.get().getAvslagstidslinjeFraUttak().filterValue(it -> it.avslagsårsak() != null).mapValue(_ -> true);
    }
}
