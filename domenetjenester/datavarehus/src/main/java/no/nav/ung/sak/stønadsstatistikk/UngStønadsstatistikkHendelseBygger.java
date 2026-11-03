package no.nav.ung.sak.stønadsstatistikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.oppdrag.kontrakt.sporing.HenvisningUtleder;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkHendelseBygger;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkSatsPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInntektPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.UngdomsprogramDeltakelsePeriode;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static no.nav.ung.sak.stønadsstatistikk.StønadstatistikkFellesUtleder.normaliser;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngStønadsstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.UNGDOMSYTELSE;
    private static final String KLASSEKODE = "UNG";

    private BehandlingRepository behandlingRepository;
    private StønadstatistikkFellesUtleder fellesUtleder;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;

    UngStønadsstatistikkHendelseBygger() {
        //for CDI proxy
    }

    @Inject
    public UngStønadsstatistikkHendelseBygger(BehandlingRepository behandlingRepository,
                                              StønadstatistikkFellesUtleder fellesUtleder,
                                              UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                              UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
                                              KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fellesUtleder = fellesUtleder;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
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
            hentDeltakelsePeriode(behandling),
            fellesUtleder.hentBehandlingsperioder(behandling),
            hentSatsPerioder(behandling),
            fellesUtleder.hentTilkjentYtelsePerioder(behandling, KLASSEKODE),
            hentInntektPerioder(behandling)
        );
    }

    private UngdomsprogramDeltakelsePeriode hentDeltakelsePeriode(Behandling behandling) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElse(null);
        if (ungdomsprogramPeriodeGrunnlag == null || ungdomsprogramPeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder().isEmpty()){
            return null;
        }
        DatoIntervallEntitet ungdomsprogramPeriode = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode();
        return new UngdomsprogramDeltakelsePeriode(ungdomsprogramPeriode.getFomDato(), ungdomsprogramPeriode.getTomDato());
    }

    private List<StønadsstatistikkSatsPeriode> hentSatsPerioder(Behandling behandling) {
        UngdomsytelseGrunnlag ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandling.getId()).orElse(null);
        if (ungdomsytelseGrunnlag == null) {
            return List.of();
        }
        return ungdomsytelseGrunnlag.getSatsTidslinje().segmenter()
            .stream().map(it -> new StønadsstatistikkSatsPeriode(
                it.getFom(),
                it.getTom(),
                map(it.getValue().satsType()),
                it.getValue().dagsats(),
                it.getValue().antallBarn(),
                it.getValue().dagsatsBarnetillegg(),
                it.getValue().grunnbeløpFaktor()))
            .sorted(Comparator.comparing(StønadsstatistikkSatsPeriode::fom))
            .toList();
    }

    private List<StønadstatistikkInntektPeriode> hentInntektPerioder(Behandling behandling) {
        return kontrollerteInntektperioderTjeneste.hentTidslinje(behandling.getId()).segmenter().stream()
            .map(it -> new StønadstatistikkInntektPeriode(
                it.getFom(),
                it.getTom(),
                normaliser(it.getValue())
            ))
            .toList();
    }

    private static StønadstatistikkSatsType map(UngdomsytelseSatsType satsType) {
        return switch (satsType) {
            case LAV -> StønadstatistikkSatsType.LAV;
            case HØY -> StønadstatistikkSatsType.HØY;
        };
    }
}
