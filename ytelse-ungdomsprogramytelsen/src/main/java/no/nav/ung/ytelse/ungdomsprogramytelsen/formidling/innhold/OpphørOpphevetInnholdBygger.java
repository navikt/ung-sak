package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.OpphørOpphevetDto;

@Dependent
public class OpphørOpphevetInnholdBygger implements VedtaksbrevInnholdBygger {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public OpphørOpphevetInnholdBygger(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        var forrigeBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Trenger forrige behandling ved opphevelse av opphør"));

        var tidligereSluttdato = ungdomsprogramPeriodeRepository.hentGrunnlag(forrigeBehandlingId)
            .map(grunnlag -> grunnlag.hentForEksaktEnPeriode().getTomDato())
            .orElseThrow(() -> new IllegalStateException("Fant ikke periodegrunnlag for forrige behandling " + forrigeBehandlingId));

        var maksdato = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(grunnlag -> grunnlag.getPeriodeMaksDato())
            .orElseThrow(() -> new IllegalStateException("Fant ikke maksdato for behandling " + behandling.getId()));

        return new TemplateInnholdResultat(TemplateType.OPPHOR_OPPHEVET,
            new OpphørOpphevetDto(tidligereSluttdato, maksdato));
    }

}
