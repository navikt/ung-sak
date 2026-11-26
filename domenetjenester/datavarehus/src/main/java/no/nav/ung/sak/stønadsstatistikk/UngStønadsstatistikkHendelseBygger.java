package no.nav.ung.sak.stønadsstatistikk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.oppdrag.kontrakt.sporing.HenvisningUtleder;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.hendelse.stønadstatistikk.StønadstatistikkHendelseBygger;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadsstatistikkSatsPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.UngStønadstatistikkHendelse;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkInngangsvilkår;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.StønadstatistikkSatsType;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.UngStønadstatistikkPeriode;
import no.nav.ung.sak.kontrakt.stønadstatistikk.dto.UngdomsprogramDeltakelsePeriode;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngStønadsstatistikkHendelseBygger implements StønadstatistikkHendelseBygger {

    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.UNGDOMSYTELSE;
    private static final String KLASSEKODE = "UNG";

    private BehandlingRepository behandlingRepository;
    private StønadstatistikkFellesUtleder fellesUtleder;
    private SøknadRepository søknadRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;

    UngStønadsstatistikkHendelseBygger() {
        //for CDI proxy
    }

    @Inject
    public UngStønadsstatistikkHendelseBygger(BehandlingRepository behandlingRepository, StønadstatistikkFellesUtleder fellesUtleder, SøknadRepository søknadRepository, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository, UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.fellesUtleder = fellesUtleder;
        this.søknadRepository = søknadRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
    }

    @Override
    public UngStønadstatistikkHendelse lagHendelse(UUID behandlingUuid) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingUuid);
        return new UngStønadstatistikkHendelse(
            YTELSE_TYPE,
            behandling.getAktørId(),
            behandling.getFagsak().getSaksnummer(),
            behandling.getUuid(),
            fellesUtleder.hentOrginalBehandlingUuidHvisEksisterer(behandling),
            hentFørsteSøknadsdato(behandling),
            fellesUtleder.hentVedtakTidspunkt(behandling),
            HenvisningUtleder.utledHenvisning(behandling.getUuid(), null),
            hentDeltakelsePeriode(behandling),
            hentBehandlingsperioder(behandling),
            hentSatsPerioder(behandling),
            fellesUtleder.hentTilkjentYtelsePerioder(behandling, KLASSEKODE),
            fellesUtleder.hentInntektPerioder(behandling)
        );
    }

    private LocalDate hentFørsteSøknadsdato(Behandling behandling) {
        List<SøknadEntitet> søknader = søknadRepository.hentSøknaderForFagsak(behandling.getFagsakId());
        return søknader.stream().map(SøknadEntitet::getMottattDato).min(Comparator.naturalOrder()).orElseThrow();
    }

    private UngdomsprogramDeltakelsePeriode hentDeltakelsePeriode(Behandling behandling) {
        UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElse(null);
        if (ungdomsprogramPeriodeGrunnlag == null || ungdomsprogramPeriodeGrunnlag.getUngdomsprogramPerioder().getPerioder().isEmpty()){
            return null;
        }
        DatoIntervallEntitet ungdomsprogramPeriode = ungdomsprogramPeriodeGrunnlag.hentForEksaktEnPeriode();
        return new UngdomsprogramDeltakelsePeriode(ungdomsprogramPeriode.getFomDato(), ungdomsprogramPeriode.getTomDato());
    }

    private List<UngStønadstatistikkPeriode> hentBehandlingsperioder(Behandling behandling) {
        return fellesUtleder.hentVilkårTidslinje(behandling).segmenter().stream()
            .map(segment -> new UngStønadstatistikkPeriode(
                segment.getFom(),
                segment.getTom(),
                StønadstatistikkFellesUtleder.samletUtfall(segment.getValue()),
                segment.getValue().entrySet().stream()
                    .map(e -> new StønadstatistikkInngangsvilkår(e.getKey(), StønadstatistikkFellesUtleder.map(e.getValue())))
                    .toList()))
            .toList();
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

    private static StønadstatistikkSatsType map(UngdomsytelseSatsType satsType) {
        return switch (satsType) {
            case LAV -> StønadstatistikkSatsType.LAV;
            case HØY -> StønadstatistikkSatsType.HØY;
        };
    }
}
