package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.OpphørOpphevetDto;

@Dependent
public class OpphørOpphevetInnholdBygger implements VedtaksbrevInnholdBygger {

    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    public OpphørOpphevetInnholdBygger(UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                        ProsessTriggereRepository prosessTriggereRepository) {
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
    }

    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> resultatTidslinje) {
        // Den tidligere opphørsdatoen (som nå oppheves) leses fra prosess-triggeren for RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM
        // på DENNE behandlingen, ikke fra forrige behandlings periodegrunnlag. Dette fungerer også når opphøret og
        // opphevelsen er slått sammen på samme (fortsatt åpne) behandling, hvor forrige behandling aldri fikk det
        // opprinnelige opphøret persistert (se UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder, som setter
        // triggerens periode.fom til dagen etter tidligere opphørsdato).
        var triggerPeriodeFom = prosessTriggereRepository.hentGrunnlag(behandling.getId())
            .flatMap(grunnlag -> grunnlag.getTriggere().stream()
                .filter(t -> t.getÅrsak() == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM)
                .map(t -> t.getPeriode().getFomDato())
                .findFirst())
            .orElseThrow(() -> new IllegalStateException("Fant ikke prosesstrigger for opphevelse av opphør på behandling " + behandling.getId()));

        var tidligereSluttdato = triggerPeriodeFom.minusDays(1);

        var maksdato = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId())
            .flatMap(grunnlag -> grunnlag.getPeriodeMaksDato())
            .orElseThrow(() -> new IllegalStateException("Fant ikke maksdato for behandling " + behandling.getId()));

        return new TemplateInnholdResultat(TemplateType.OPPHOR_OPPHEVET,
            new OpphørOpphevetDto(tidligereSluttdato, maksdato));
    }

}
