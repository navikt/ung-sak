package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato.MaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

import java.util.List;

/**
 * Håndterer behandlinger som har (blant sine årsaker) at et tidligere opphør av ungdomsprogrammet er opphevet.
 * En ventende uttalelse om endret sluttdato blir alltid avbrutt, siden opphevelsen fjerner sluttdatoen (uendret
 * maksdato) — dette gjelder også om behandlingen samtidig har den nå utdaterte årsaken
 * RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, se {@link #harKunOpphørsÅrsaker}. Dersom opphevelsen er eneste reelle
 * årsak, skal ingen ytterligere etterlysninger opprettes: bruker har allerede vært i kontakt med Nav i forkant
 * (f.eks. medhold i klage), og skal ikke varsles på nytt om at programperioden er gjenåpnet.
 */
@ApplicationScoped
public class OpphevelseAvOpphørEtterlysningHåndterer {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste;

    OpphevelseAvOpphørEtterlysningHåndterer() {
    }

    @Inject
    public OpphevelseAvOpphørEtterlysningHåndterer(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste,
                                                    ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste,
                                                    MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.maksdatoEtterlysningTjeneste = maksdatoEtterlysningTjeneste;
    }

    public void håndter(BehandlingReferanse behandlingReferanse, List<BehandlingÅrsakType> årsaker) {
        programperiodeendringEtterlysningTjeneste.avbrytVentendeSluttdatoOgPeriodeEtterlysninger(behandlingReferanse);

        if (harKunOpphørsÅrsaker(årsaker)) {
            return;
        }

        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    /**
     * Sant når eneste (relevante) årsak er opphevelse av opphør, eventuelt sammen med den nå utdaterte
     * RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM (artefakt av at hendelsene kan slås sammen på samme åpne behandling,
     * se OpprettRevurderingEllerOpprettDiffTask). Andre årsaker (f.eks. inntektskontroll) regnes som reelle
     * tilleggsårsaker.
     */
    private boolean harKunOpphørsÅrsaker(List<BehandlingÅrsakType> årsaker) {
        return årsaker.stream().allMatch(å -> å == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM
            || å == BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM);
    }
}
