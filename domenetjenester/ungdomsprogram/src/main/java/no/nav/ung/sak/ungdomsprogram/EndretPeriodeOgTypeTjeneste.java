package no.nav.ung.sak.ungdomsprogram;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste.EndretDato;

import java.util.List;
import java.util.Optional;

@Dependent
public class EndretPeriodeOgTypeTjeneste {
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public EndretPeriodeOgTypeTjeneste(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    public Optional<EndretPeriodeOgType> finnEndretPeriodeDatoOgEndringType(Etterlysning etterlysning) {
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlagFraGrunnlagsReferanse(etterlysning.getGrunnlagsreferanse());
        var initiellPeriodegrunnlag = ungdomsprogramPeriodeRepository.hentInitiell(etterlysning.getBehandlingId()).orElseThrow(() -> new IllegalStateException("Skal ha innhentet initiell programperiodegrunnlag for behandling " + etterlysning.getBehandlingId()));
        List<EndretDato> endretStartDatoer = UngdomsprogramPeriodeTjeneste.finnEndretStartdatoer(ungdomsprogramPeriodeGrunnlag, initiellPeriodegrunnlag);
        List<EndretDato> endretSluttDatoer = UngdomsprogramPeriodeTjeneste.finnEndretSluttdatoer(ungdomsprogramPeriodeGrunnlag, initiellPeriodegrunnlag);

        if (!endretStartDatoer.isEmpty() && endretSluttDatoer.isEmpty()) {
            return Optional.of(new EndretPeriodeOgType(EndringType.ENDRET_STARTDATO, endretStartDatoer));
        }
        else if (endretStartDatoer.isEmpty() && !endretSluttDatoer.isEmpty()) {
            return Optional.of(new EndretPeriodeOgType(EndringType.ENDRET_SLUTTDATO, endretSluttDatoer));
        }
        else if (!endretStartDatoer.isEmpty()) {
            return Optional.of(new EndretPeriodeOgType(EndringType.ENDRET_PROGRAMPERIODE, endretStartDatoer, endretSluttDatoer));
        }
        else {
           return Optional.empty();
        }

    }
}
