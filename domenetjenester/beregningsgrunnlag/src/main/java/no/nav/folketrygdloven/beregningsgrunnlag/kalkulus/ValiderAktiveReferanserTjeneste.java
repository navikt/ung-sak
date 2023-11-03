package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningsgrunnlagPeriode;

@Dependent
public class ValiderAktiveReferanserTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ValiderAktiveReferanserTjeneste.class);
    private final Instance<KalkulusApiTjeneste> kalkulusTjenester;
    private final BeregningPerioderGrunnlagRepository grunnlagRepository;

    @Inject
    public ValiderAktiveReferanserTjeneste(@Any Instance<KalkulusApiTjeneste> kalkulusTjenester,
                                           BeregningPerioderGrunnlagRepository grunnlagRepository) {
        this.kalkulusTjenester = kalkulusTjenester;
        this.grunnlagRepository = grunnlagRepository;
    }


    public void validerIngenLøseReferanser(BehandlingReferanse behandlingReferanse) {
        var aktiveReferanserIKalkulus = finnTjeneste(behandlingReferanse.getFagsakYtelseType()).hentReferanserMedAktiveGrunnlag(behandlingReferanse.getSaksnummer());
        var referanserOpprettetIGjeldendeBehandling = finnReferanserOpprettetIGjeldendeBehandling(behandlingReferanse);
        var referanserSomBurdeHaBlittFjernet = aktiveReferanserIKalkulus.stream()
            .filter(kalkulusRef -> erOpprettetIBehandlingenMenErIkkeAktivIK9(kalkulusRef, referanserOpprettetIGjeldendeBehandling)).collect(Collectors.toSet());
        if (!referanserSomBurdeHaBlittFjernet.isEmpty()) {
            log.warn("Fant referanser for behandlingen som ikke er ryddet: " + referanserSomBurdeHaBlittFjernet);
        }
    }

    private static boolean erOpprettetIBehandlingenMenErIkkeAktivIK9(UUID kalkulusRef, Set<HistoriskReferanse> referanserOpprettetIGjeldendeBehandling) {
        return referanserOpprettetIGjeldendeBehandling.stream().anyMatch(r -> r.ref().equals(kalkulusRef) && !r.aktiv());
    }

    private Set<HistoriskReferanse> finnReferanserOpprettetIGjeldendeBehandling(BehandlingReferanse behandlingReferanse) {
        Set<HistoriskReferanse> referanserOpprettetIGjeldendeBehandling = new HashSet<>();

        var initielleReferanser = grunnlagRepository.getInitiellVersjon(behandlingReferanse.getBehandlingId())
            .stream()
            .flatMap(bg -> bg.getGrunnlagPerioder().stream())
            .map(BeregningsgrunnlagPeriode::getEksternReferanse)
            .collect(Collectors.toSet());

        grunnlagRepository.hentAlleHistoriskeReferanserForBehandling(behandlingReferanse.getBehandlingId()).forEach(it -> {
            var eksisterende = referanserOpprettetIGjeldendeBehandling.stream().filter(r -> r.ref().equals(it.getElement1())).findFirst();
            var erIkkeInitiell = initielleReferanser.stream().noneMatch(ir -> ir.equals(it.getElement1()));
            if (eksisterende.isEmpty() || !eksisterende.get().aktiv() && erIkkeInitiell) {
                referanserOpprettetIGjeldendeBehandling.add(new HistoriskReferanse(it.getElement1(), it.getElement2()));
            }
        });
        return referanserOpprettetIGjeldendeBehandling;
    }

    private KalkulusApiTjeneste finnTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(kalkulusTjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste for " + fagsakYtelseType));
    }

    record HistoriskReferanse(UUID ref, boolean aktiv) {
    }

}
