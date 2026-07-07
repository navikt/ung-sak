package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningOppretter;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.KontrollerInntektEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.maksdato.MaksdatoEtterlysningTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll.ProgramperiodeendringEtterlysningTjeneste;

import java.util.List;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngEtterlysningOppretter implements EtterlysningOppretter {

    private KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste;
    private ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste;
    private MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste;
    private BehandlingRepository behandlingRepository;

    public UngEtterlysningOppretter() {
    }

    @Inject
    public UngEtterlysningOppretter(KontrollerInntektEtterlysningTjeneste kontrollerInntektEtterlysningTjeneste, ProgramperiodeendringEtterlysningTjeneste programperiodeendringEtterlysningTjeneste, MaksdatoEtterlysningTjeneste maksdatoEtterlysningTjeneste, BehandlingRepository behandlingRepository) {
        this.kontrollerInntektEtterlysningTjeneste = kontrollerInntektEtterlysningTjeneste;
        this.programperiodeendringEtterlysningTjeneste = programperiodeendringEtterlysningTjeneste;
        this.maksdatoEtterlysningTjeneste = maksdatoEtterlysningTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public void opprettEtterlysninger(BehandlingReferanse behandlingReferanse) {
        var årsaker = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId()).getBehandlingÅrsakerTyper();

        // Rene varsel-om-opphør-ved-maksdato-behandlinger skal kun varsle om opphør, og ikke trigge
        // inntektskontroll eller programperiodeendring-varsling som del av opphørsløpet.
        if (erRentVarselOpphørVedMaksdatoLøp(årsaker)) {
            maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
            return;
        }

        if (årsaker.contains(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM)) {
            håndterOpphevelseAvOpphør(behandlingReferanse, årsaker);
            return;
        }

        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        programperiodeendringEtterlysningTjeneste.opprettEtterlysningerForProgramperiodeEndring(behandlingReferanse);
        maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    /**
     * Håndterer behandlinger som har (blant sine årsaker) at et tidligere opphør av ungdomsprogrammet er opphevet.
     * En ventende uttalelse om endret sluttdato blir alltid avbrutt, siden opphevelsen fjerner sluttdatoen (uendret
     * maksdato) — dette gjelder også om behandlingen samtidig har den nå utdaterte årsaken
     * RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, se {@link #harKunOpphørsÅrsaker}. Dersom opphevelsen er eneste reelle
     * årsak, skal ingen ytterligere etterlysninger opprettes: bruker har allerede vært i kontakt med Nav i forkant
     * (f.eks. medhold i klage), og skal ikke varsles på nytt om at programperioden er gjenåpnet.
     */
    private void håndterOpphevelseAvOpphør(BehandlingReferanse behandlingReferanse, List<BehandlingÅrsakType> årsaker) {
        programperiodeendringEtterlysningTjeneste.avbrytVentendeSluttdatoEtterlysninger(behandlingReferanse);

        if (harKunOpphørsÅrsaker(årsaker)) {
            return;
        }

        kontrollerInntektEtterlysningTjeneste.opprettEtterlysninger(behandlingReferanse);
        maksdatoEtterlysningTjeneste.opprettEtterlysningForOpphørVedMaksdatoDersomRelevant(behandlingReferanse);
    }

    private boolean erRentVarselOpphørVedMaksdatoLøp(List<BehandlingÅrsakType> årsaker) {
        return !årsaker.isEmpty() && årsaker.stream().allMatch(å -> å == BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO);
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
