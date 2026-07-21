package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Håndterer hendelse om at et tidligere sendt opphør av ungdomsprogrammet er opphevet, f.eks. fordi opphørsdato
 * ble satt feil, eller fordi bruker har fått medhold i klage på opphøret.
 * <p>
 * Maksdato for programperioden er uendret av denne hendelsen (den forholder seg kun til startdato). Selve
 * gjenåpningen av programperioden skjer automatisk ved re-innhenting av periodegrunnlag fra register i den
 * påfølgende revurderingen (se {@code InnhentUngdomsprogramperioderTask}) — denne klassen skal derfor kun avgjøre
 * hvilken fagsak/periode som skal revurderes, ikke selve periodeinnholdet.
 */
@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_OPPHØR_OPPHEVET")
public class UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private static final Logger logger = LoggerFactory.getLogger(UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder.class);
    private BehandlingRepository behandlingRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste;

    public UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramOpphørOpphevetFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                                  UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste,
                                                                  FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.finnFagsakerForAktørTjeneste = finnFagsakerForAktørTjeneste;
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        List<AktørId> aktører = hendelse.getHendelseInfo().getAktørIder();
        LocalDate tidligereOpphørsdato = hendelse.getHendelsePeriode().getFom();
        String hendelseId = hendelse.getHendelseInfo().getHendelseId();

        var fagsaker = new HashMap<Fagsak, List<ÅrsakOgPerioder>>();

        for (AktørId aktør : aktører) {
            var relevantFagsak = finnFagsakerForAktørTjeneste
                .hentRelevantFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør, tidligereOpphørsdato)
                .or(() -> finnFagsakerForAktørTjeneste.hentSisteFagsakForAktørSomSøker(FagsakYtelseType.UNGDOMSYTELSE, aktør));
            if (relevantFagsak.isEmpty()) {
                logger.info("Ingen relevant fagsak funnet for tidligere opphørsdato {} og hendelse {}.", tidligereOpphørsdato, hendelseId);
                continue;
            }

            Saksnummer saksnummer = relevantFagsak.get().getSaksnummer();
            Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(relevantFagsak.get().getId());
            if (behandlingOpt.isEmpty()) {
                logger.info("Det er ingen behandling på sak {}. Ignorerer hendelse {}.", saksnummer, hendelseId);
                continue;
            }

            Behandling sisteBehandling = behandlingOpt.get();

            if (erAlleredeHåndtertIGrunnlag(sisteBehandling, tidligereOpphørsdato, hendelseId, saksnummer)) {
                continue;
            }

            var periode = utledPeriode(relevantFagsak.get(), sisteBehandling, tidligereOpphørsdato);
            logger.info("Oppretter revurdering for sak {} grunnet opphevelse av opphør fra hendelse {} med tidligere opphørsdato {}.", saksnummer, hendelseId, tidligereOpphørsdato);
            fagsaker.put(relevantFagsak.get(), List.of(new ÅrsakOgPerioder(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM, periode)));
        }

        return fagsaker;
    }

    /**
     * Idempotens-sjekk: dersom periodegrunnlaget allerede strekker seg forbi den tidligere opphørsdatoen, er
     * opphevelsen allerede håndtert (f.eks. fra en tidligere behandling av samme hendelse), og hendelsen skal
     * ignoreres.
     */
    private boolean erAlleredeHåndtertIGrunnlag(Behandling behandling, LocalDate tidligereOpphørsdato, String hendelseId, Saksnummer saksnummer) {
        var tidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandling.getId());
        if (tidslinje.isEmpty()) {
            return false;
        }
        boolean alleredeHåndtert = tidslinje.getMaxLocalDate().isAfter(tidligereOpphørsdato);
        if (alleredeHåndtert) {
            logger.info("Periodegrunnlag på behandling {} for {} strekker seg allerede forbi tidligere opphørsdato {}. Ignorerer hendelse {}.",
                behandling.getUuid(), saksnummer, tidligereOpphørsdato, hendelseId);
        }
        return alleredeHåndtert;
    }

    /**
     * Perioden som blir gjenåpnet strekker seg fra dagen etter den tidligere opphørsdatoen og frem til
     * (uendret) periodeMaksDato. periodeMaksDato er kilde til sannhet og forventes alltid å finnes i
     * grunnlaget på dette tidspunktet — mangler den, er det en feiltilstand som skal feile hardt fremfor
     * å falle tilbake til en potensielt feil verdi.
     */
    private DatoIntervallEntitet utledPeriode(Fagsak fagsak, Behandling behandling, LocalDate tidligereOpphørsdato) {
        var tom = ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ikke periodeMaksDato for behandling %s på sak %s. Kan ikke utlede periode for opphevelse av opphør."
                .formatted(behandling.getUuid(), fagsak.getSaksnummer())));
        if (!tom.isAfter(tidligereOpphørsdato)) {
            throw new IllegalStateException("Maksdato/tom (%s) er ikke etter tidligere opphørsdato (%s) — hendelsen burde ha blitt ignorert."
                .formatted(tom, tidligereOpphørsdato));
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(tidligereOpphørsdato.plusDays(1), tom);
    }

}
