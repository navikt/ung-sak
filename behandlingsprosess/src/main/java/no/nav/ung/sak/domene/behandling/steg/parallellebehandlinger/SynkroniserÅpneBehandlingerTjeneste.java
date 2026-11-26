package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.varsel.EndringType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallellBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;

/**
 * Oppdaterer åpne ytelsesbehandlinger med vedtatte data og tar dem av vent. Se {@link SynkroniserÅpneBehandlingerTask}.
 * <p>
 * Kilden er siste avsluttede, ikke-henlagte ytelsesbehandling (A). For hver åpen behandling (B) som ikke bygger på A:
 * <ol>
 * <li>B kan ikke ha vedtaksprioritet eller vedtak. Ellers feiler synkroniseringen, fordi B da kan ha simulert eller
 * beregnet tilkjent ytelse mot en annen originalbehandling.</li>
 * <li>Taskgruppen for gjenoppta, diff og fortsett bygges. Snapshotet til diffen tas her, før kopieringen, slik at
 * diffen fanger endringene og avgjør tilbakehoppet med dagens startpunktutledning.</li>
 * <li>Data kopieres fra A til B etter eierskap, og eksisterende data på B erstattes. Kontrollbehandlingen eier
 * kontrollperioder og inntektsuttalelser. Ordinær behandling eier resten.</li>
 * <li>{@code originalBehandlingId} på B settes til A, og B får et historikkinnslag.</li>
 * <li>En kontrollbehandling uten måneder igjen å kontrollere får {@link HenleggKontrollbehandlingUtenMånederTask} først
 * i gruppen.</li>
 * </ol>
 * Behandlinger som bygger på A og venter i ventepunktet for vedtaksprioritet, tas bare av vent.
 * <p>
 * Har en behandling uferdige tasker i rekkefølge, utsettes hele synkroniseringen til de er kjørt. Tasken feiler ikke,
 * fordi tasker som er laget etter synkroniseringen, ellers ville ventet på den og låst taskkøen på fagsaken.
 */
@Dependent
public class SynkroniserÅpneBehandlingerTjeneste {

    static final String HISTORIKK_TITTEL = "Behandlingen er oppdatert med nytt vedtak";

    private static final Logger log = LoggerFactory.getLogger(SynkroniserÅpneBehandlingerTjeneste.class);

    private static final Set<EndringType> KONTROLLBEHANDLINGENS_UTTALELSER = EnumSet.of(EndringType.ENDRET_INNTEKT);
    private static final Set<EndringType> ORDINÆR_BEHANDLINGS_UTTALELSER = EnumSet.complementOf(EnumSet.copyOf(KONTROLLBEHANDLINGENS_UTTALELSER));

    private BehandlingRepository behandlingRepository;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private VedtaksprioritetTjeneste vedtaksprioritetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private SynkroniserÅpneBehandlingerTaskOppretter synkroniserÅpneBehandlingerTaskOppretter;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private UttalelseRepository uttalelseRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private StartdatoRepository startdatoRepository;
    private PersonopplysningRepository personopplysningRepository;
    private Instance<YtelsesgrunnlagSynkroniserer> ytelsesgrunnlagSynkroniserere;

    SynkroniserÅpneBehandlingerTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SynkroniserÅpneBehandlingerTjeneste(BehandlingRepository behandlingRepository,
                                               ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                               VedtaksprioritetTjeneste vedtaksprioritetTjeneste,
                                               BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                               FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                               SynkroniserÅpneBehandlingerTaskOppretter synkroniserÅpneBehandlingerTaskOppretter,
                                               RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder,
                                               HistorikkinnslagRepository historikkinnslagRepository,
                                               BehandlingVedtakRepository behandlingVedtakRepository,
                                               VilkårResultatRepository vilkårResultatRepository,
                                               UttalelseRepository uttalelseRepository,
                                               TilkjentYtelseRepository tilkjentYtelseRepository,
                                               StartdatoRepository startdatoRepository,
                                               PersonopplysningRepository personopplysningRepository,
                                               @Any Instance<YtelsesgrunnlagSynkroniserer> ytelsesgrunnlagSynkroniserere) {
        this.behandlingRepository = behandlingRepository;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.vedtaksprioritetTjeneste = vedtaksprioritetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.synkroniserÅpneBehandlingerTaskOppretter = synkroniserÅpneBehandlingerTaskOppretter;
        this.relevanteKontrollperioderUtleder = relevanteKontrollperioderUtleder;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.uttalelseRepository = uttalelseRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.startdatoRepository = startdatoRepository;
        this.personopplysningRepository = personopplysningRepository;
        this.ytelsesgrunnlagSynkroniserere = ytelsesgrunnlagSynkroniserere;
    }

    public void synkroniser(ProsessTaskData prosessTaskData) {
        Long fagsakId = prosessTaskData.getFagsakId();
        Optional<Behandling> kilde = parallelleBehandlingerRepository.finnSisteAvsluttedeIkkeHenlagteYtelsesbehandlingId(fagsakId)
            .map(behandlingRepository::hentBehandling);

        List<Behandling> skalSynkroniseres = new ArrayList<>();
        List<Behandling> skalVekkes = new ArrayList<>();
        for (ParallellBehandling åpen : parallelleBehandlingerRepository.hentIkkeAvsluttedeYtelsesbehandlinger(fagsakId)) {
            Behandling behandling = behandlingRepository.hentBehandling(åpen.behandlingId());
            if (kilde.isPresent() && !vedtaksprioritetTjeneste.byggerPåSisteVedtak(fagsakId, åpen.behandlingId())) {
                validerKanSynkroniseres(behandling, åpen);
                skalSynkroniseres.add(behandling);
            } else if (venterIVentepunktForVedtaksprioritet(behandling)) {
                skalVekkes.add(behandling);
            }
        }

        if (harUferdigeTasker(fagsakId, skalSynkroniseres) || harUferdigeTasker(fagsakId, skalVekkes)) {
            log.info("Utsetter synkronisering av åpne behandlinger fordi {} behandling(er) har uferdige tasker", skalSynkroniseres.size() + skalVekkes.size());
            synkroniserÅpneBehandlingerTaskOppretter.opprett(fagsakId);
            return;
        }

        for (Behandling behandling : skalSynkroniseres) {
            synkroniser(kilde.orElseThrow(), behandling);
        }
        for (Behandling behandling : skalVekkes) {
            log.info("Gjenopptar behandling {} som venter i ventepunktet for vedtaksprioritet", behandling.getId());
            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false);
        }
    }

    private void synkroniser(Behandling kilde, Behandling mål) {
        log.info("Oppdaterer behandling {} med vedtatte data fra behandling {}", mål.getId(), kilde.getId());

        // Snapshotet til diffen tas når gruppen bygges, og må derfor tas før kopieringen.
        ProsessTaskGruppe gjenopptaGruppe = behandlingProsesseringTjeneste.opprettTaskGruppeForSynkronisering(mål);
        if (gjenopptaGruppe == null) {
            throw new IllegalStateException("Fikk ikke laget taskgruppe for å gjenoppta behandling " + mål.getId() + " etter synkronisering");
        }

        kopierEtterEierskap(kilde, mål);

        mål.oppdaterOriginalBehandling(kilde);
        behandlingRepository.lagre(mål, behandlingRepository.taSkriveLås(mål));
        lagHistorikkinnslag(kilde, mål);

        ProsessTaskGruppe gruppe = gjenopptaGruppe;
        if (mål.erKontrollbehandling() && !HenleggKontrollbehandlingUtenMånederTask.harMånederÅKontrollere(relevanteKontrollperioderUtleder, mål.getId())) {
            log.info("Kontrollbehandling {} har ingen måneder å kontrollere etter oppdatering, og vurderes henlagt", mål.getId());
            gruppe = medHenleggelseFørst(HenleggKontrollbehandlingUtenMånederTask.lagTask(mål.getFagsakId(), mål.getId()), gjenopptaGruppe);
        }
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(mål.getFagsakId(), mål.getId(), gruppe);
    }

    private void kopierEtterEierskap(Behandling kilde, Behandling mål) {
        Long fra = kilde.getId();
        Long til = mål.getId();
        if (kilde.erOrdinærYtelseBehandling() && mål.erKontrollbehandling()) {
            personopplysningRepository.kopierGrunnlagFraEksisterendeBehandling(fra, til);
            startdatoRepository.erstattGrunnlagMedKopiFra(fra, til);
            vilkårResultatRepository.erstattMedKopiFra(fra, til);
            uttalelseRepository.erstattUttalelserMedKopiFra(fra, til, ORDINÆR_BEHANDLINGS_UTTALELSER);
            YtelsesgrunnlagSynkroniserer.finnTjeneste(ytelsesgrunnlagSynkroniserere, mål.getFagsakYtelseType())
                .erstattYtelsesgrunnlagFraOrdinærBehandling(fra, til);
        } else if (kilde.erKontrollbehandling() && mål.erOrdinærYtelseBehandling()) {
            tilkjentYtelseRepository.erstattKontrollPerioderMedKopiFra(fra, til);
            uttalelseRepository.erstattUttalelserMedKopiFra(fra, til, KONTROLLBEHANDLINGENS_UTTALELSER);
            // TODO kontrollbehandling: kopier rapportert inntekt (OPPGITT_OPPTJENING i abakus). Avventer avklaring med k9-abakus.
        } else {
            throw new IllegalStateException("Kan ikke synkronisere behandling " + til + " av type " + mål.getType().getKode()
                + " med vedtatte data fra behandling " + fra + " av type " + kilde.getType().getKode()
                + ". Fagsaken kan bare ha én åpen ordinær behandling og én åpen kontrollbehandling.");
        }
    }

    private void validerKanSynkroniseres(Behandling behandling, ParallellBehandling åpen) {
        if (vedtaksprioritetTjeneste.erForbiVentepunktForVedtaksprioritet(behandling.getFagsakYtelseType(), åpen)
            || behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).isPresent()) {
            throw new IllegalStateException("Behandling " + behandling.getId() + " bygger ikke på siste vedtak, men er forbi "
                + VedtaksprioritetTjeneste.FØRSTE_STEG_MED_VEDTAKSPRIORITET.getKode() + " eller har vedtak. Behandlingen kan ikke synkroniseres.");
        }
    }

    private boolean harUferdigeTasker(Long fagsakId, List<Behandling> behandlinger) {
        return behandlinger.stream()
            .anyMatch(b -> !fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(fagsakId, b.getId(), null).isEmpty());
    }

    private static boolean venterIVentepunktForVedtaksprioritet(Behandling behandling) {
        return behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_VEDTAKSPRIORITET);
    }

    private void lagHistorikkinnslag(Behandling kilde, Behandling mål) {
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medFagsakId(mål.getFagsakId())
            .medBehandlingId(mål.getId())
            .medTittel(HISTORIKK_TITTEL)
            .addLinje("Behandlingen bygger nå på vedtaket i " + kilde.getType().getNavn().toLowerCase(Locale.ROOT) + ".")
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    /**
     * Legger henleggelsestasken først og beholder rekkefølgen og parallelliteten i gruppen for gjenoppta, diff og fortsett.
     */
    static ProsessTaskGruppe medHenleggelseFørst(ProsessTaskData henleggelse, ProsessTaskGruppe gjenopptaGruppe) {
        Map<String, List<ProsessTaskData>> perSekvens = new LinkedHashMap<>();
        for (ProsessTaskGruppe.Entry entry : gjenopptaGruppe.getTasks()) {
            perSekvens.computeIfAbsent(entry.getSekvens(), s -> new ArrayList<>()).add(entry.getTask());
        }
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        gruppe.addNesteSekvensiell(henleggelse);
        for (List<ProsessTaskData> tasker : perSekvens.values()) {
            if (tasker.size() == 1) {
                gruppe.addNesteSekvensiell(tasker.getFirst());
            } else {
                gruppe.addNesteParallell(tasker);
            }
        }
        return gruppe;
    }
}
