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
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.AktivitetspengerStønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.AktivitetspengerStønadstatistikkPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class AktivitetspengerStønadsstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.AKTIVITETSPENGER;
    private static final String KLASSEKODE = "AKTIVITETPENGUNG";

    private BehandlingRepository behandlingRepository;
    private StønadstatistikkFellesUtleder fellesUtleder;
    private AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;

    AktivitetspengerStønadsstatistikkHendelseBygger() {
        //for CDI proxy
    }

    @Inject
    public AktivitetspengerStønadsstatistikkHendelseBygger(BehandlingRepository behandlingRepository,
                                                           StønadstatistikkFellesUtleder fellesUtleder,
                                                           AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fellesUtleder = fellesUtleder;
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
    }

    @Override
    public AktivitetspengerStønadstatistikkHendelse lagHendelse(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        return new AktivitetspengerStønadstatistikkHendelse(
            YTELSE_TYPE,
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            fellesUtleder.hentOrginalBehandlingUuidHvisEksisterer(behandling),
            fellesUtleder.hentVedtakTidspunkt(behandling),
            HenvisningUtleder.utledHenvisning(behandling.getUuid(), null),
            hentBehandlingsperioder(behandling),
            hentSatsPerioder(behandling),
            fellesUtleder.hentTilkjentYtelsePerioder(behandling, KLASSEKODE),
            fellesUtleder.hentInntektPerioder(behandling)
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

    private List<AktivitetspengerStønadstatistikkPeriode> hentBehandlingsperioder(Behandling behandling) {
        return fellesUtleder.hentVilkårTidslinje(behandling).segmenter().stream()
            .map(segment -> new AktivitetspengerStønadstatistikkPeriode(
                segment.getFom(),
                segment.getTom(),
                StønadstatistikkFellesUtleder.samletUtfall(segment.getValue())))
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
