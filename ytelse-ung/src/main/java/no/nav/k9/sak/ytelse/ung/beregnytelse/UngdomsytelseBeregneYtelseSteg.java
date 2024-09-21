package no.nav.k9.sak.ytelse.ung.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseGrunnlag;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseGrunnlagRepository;
import no.nav.k9.sak.ytelse.ung.beregning.UngdomsytelseSatser;

@FagsakYtelseTypeRef(UNGDOMSYTELSE)
@BehandlingStegRef(value = BEREGN_YTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseBeregneYtelseSteg implements BeregneYtelseSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    protected UngdomsytelseBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public UngdomsytelseBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                          UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId);
        var satsTidslinje = ungdomsytelseGrunnlag.map(UngdomsytelseGrunnlag::getSatsTidslinje).orElse(LocalDateTimeline.empty());


        var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
            .medRegelInput(mapTilRegelInput(satsTidslinje))
            .medRegelSporing(mapTilRegelInput(satsTidslinje))
            .build();

        satsTidslinje.toSegments().forEach(p -> {
            var resultatPeriode = BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(p.getFom(), p.getTom())
                .build(beregningsresultatEntitet);
            BeregningsresultatAndel.builder()
                .medDagsats(p.getValue().dagsats().setScale(0, RoundingMode.HALF_UP).intValue())
                .medDagsatsFraBg(p.getValue().dagsats().setScale(0, RoundingMode.HALF_UP).intValue()) // TODO: Denne er ikkje nødvendig for UNG, men er påkrevd
                .medInntektskategori(Inntektskategori.ARBEIDSTAKER_UTEN_FERIEPENGER)
                .medUtbetalingsgradOppdrag(BigDecimal.valueOf(100))
                .medUtbetalingsgrad(BigDecimal.valueOf(100))
                .medStillingsprosent(BigDecimal.ZERO) // TODO: Denne var påkrevd i resultatandel, men gir ikkje meining for UNG-ytelsen
                .medBrukerErMottaker(true)
                .buildFor(resultatPeriode);
        });

        beregningsresultatRepository.lagre(behandling, beregningsresultatEntitet);


        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private String mapTilRegelInput(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje) {
        return satsTidslinje.toString();
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }

}
