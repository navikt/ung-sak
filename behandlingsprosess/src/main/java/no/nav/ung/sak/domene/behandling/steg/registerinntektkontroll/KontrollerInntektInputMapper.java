package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.EtterlysningTjeneste;
import no.nav.ung.sak.uttalelse.EtterlysningInfo;
import no.nav.ung.sak.uttalelse.EtterlysningsPeriode;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.kontroll.RelevanteKontrollperioderUtleder;

import java.util.List;

@Dependent
public class KontrollerInntektInputMapper {

    private RapportertInntektMapper rapportertInntektMapper;
    private EtterlysningTjeneste etterlysningTjeneste;
    private RelevanteKontrollperioderUtleder kontrollperioderUtleder;

    @Inject
    public KontrollerInntektInputMapper(RapportertInntektMapper rapportertInntektMapper,
                                        EtterlysningTjeneste etterlysningTjeneste,
                                        RelevanteKontrollperioderUtleder kontrollperioderUtleder) {
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.etterlysningTjeneste = etterlysningTjeneste;
        this.kontrollperioderUtleder = kontrollperioderUtleder;
    }

    public KontrollerInntektInput mapInput(BehandlingReferanse behandlingReferanse) {
        var årsakTidslinje = kontrollperioderUtleder.utledPerioderForKontrollAvInntekt(behandlingReferanse.getBehandlingId());
        var rapporterteInntekterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(behandlingReferanse.getBehandlingId());
        var eksisterendeEtterlysninger = etterlysningTjeneste.hentGjeldendeEtterlysninger(behandlingReferanse.getBehandlingId(), behandlingReferanse.getFagsakId(), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        var eksisterendeEtterlysningOgGrunnlagTidslinje = finnGjeldendeEtterlysningTidslinje(eksisterendeEtterlysninger, behandlingReferanse);

        return new KontrollerInntektInput(
            årsakTidslinje.mapValue(it -> true),
            rapporterteInntekterTidslinje,
            eksisterendeEtterlysningOgGrunnlagTidslinje
        );
    }


    private LocalDateTimeline<EtterlysningOgRegisterinntekt> finnGjeldendeEtterlysningTidslinje(List<EtterlysningData> etterlysninger, BehandlingReferanse behandlingReferanse) {
        var etterlysningsperioder = etterlysninger.stream()
            .map(it -> new EtterlysningsPeriode(
                it.periode().toLocalDateInterval(),
                new EtterlysningInfo(it.status(), it.uttalelseData() != null ? it.uttalelseData().harUttalelse() : null),
                it.grunnlagsreferanse())).toList();
        return rapportertInntektMapper.finnRegisterinntekterForEtterlysninger(behandlingReferanse.getBehandlingId(), etterlysningsperioder);
    }

}
