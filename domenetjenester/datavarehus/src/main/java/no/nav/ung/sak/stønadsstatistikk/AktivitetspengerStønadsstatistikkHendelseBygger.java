package no.nav.ung.sak.stønadsstatistikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.oppdrag.kontrakt.sporing.HenvisningUtleder;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkHendelseBygger;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsType;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkSatsPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInntektPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static no.nav.ung.sak.stønadsstatistikk.StønadstatistikkFellesUtleder.normaliser;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerStønadsstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.AKTIVITETSPENGER;
    private static final String KLASSEKODE = "AKTIVITETPENGUNG"; // Er klassekode riktig?

    private BehandlingRepository behandlingRepository;
    private StønadstatistikkFellesUtleder fellesUtleder;
    private AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;

    AktivitetspengerStønadsstatistikkHendelseBygger() {
        //for CDI proxy
    }

    @Inject
    public AktivitetspengerStønadsstatistikkHendelseBygger(BehandlingRepository behandlingRepository,
                                                           StønadstatistikkFellesUtleder fellesUtleder,
                                                           AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
                                                           KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fellesUtleder = fellesUtleder;
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
    }

    @Override
    public StønadstatistikkHendelse lagHendelse(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        return new StønadstatistikkHendelse(
            YTELSE_TYPE,
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            fellesUtleder.hentOrginalBehandlingUuidHvisEksisterer(behandling),
            fellesUtleder.hentFørsteSøknadsdato(behandling),
            fellesUtleder.hentVedtakTidspunkt(behandling),
            HenvisningUtleder.utledHenvisning(behandling.getUuid(), null),
            null, // aktivitetspenger har ikke deltakelse i ungdomsprogrammet
            fellesUtleder.hentBehandlingsperioder(behandling),
            hentSatsPerioder(behandling),
            fellesUtleder.hentTilkjentYtelsePerioder(behandling, KLASSEKODE),
            hentInntektPerioder(behandling)
        );
    }

    private List<StønadsstatistikkSatsPeriode> hentSatsPerioder(Behandling behandling) {
        AktivitetspengerGrunnlag grunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(behandling.getId()).orElse(null);
        // grunnlaget er ikke komplett dersom behandlingen er avslått før beregning
        if (grunnlag == null || grunnlag.getSatsperioder() == null || grunnlag.getBeregningsgrunnlag().isEmpty()) {
            return List.of();
        }
        return grunnlag.hentAktivitetspengerSatsTidslinje().segmenter().stream()
            .map(AktivitetspengerStønadsstatistikkHendelseBygger::map)
            .sorted(Comparator.comparing(StønadsstatistikkSatsPeriode::fom))
            .toList();
    }

    static StønadsstatistikkSatsPeriode map(LocalDateSegment<AktivitetspengerSatser> segment) {
        AktivitetspengerSatser satser = segment.getValue();
        StønadstatistikkSatsType satsType = map(satser.hentSatsType());
        return new StønadsstatistikkSatsPeriode(
            segment.getFom(),
            segment.getTom(),
            satsType,
            satser.hentBeregnetSats().dagsats(),
            satser.satsGrunnlag().antallBarn(),
            satser.satsGrunnlag().dagsatsBarnetillegg(),
            satsType == StønadstatistikkSatsType.BEREGNINGSGRUNNLAG ? null : satser.satsGrunnlag().grunnbeløpFaktor());
    }

    private List<StønadstatistikkInntektPeriode> hentInntektPerioder(Behandling behandling) {
        return kontrollerteInntektperioderTjeneste.hentTidslinje(behandling.getId()).segmenter().stream()
            .map(periode -> new StønadstatistikkInntektPeriode(
                periode.getFom(),
                periode.getTom(),
                normaliser(periode.getValue())
            ))
            .toList();
    }

    private static StønadstatistikkSatsType map(AktivitetspengerSatsType satsType) {
        return switch (satsType) {
            case LAV -> StønadstatistikkSatsType.LAV;
            case HØY -> StønadstatistikkSatsType.HØY;
            case BEREGNINGSGRUNNLAG -> StønadstatistikkSatsType.BEREGNINGSGRUNNLAG;
        };
    }
}
