package no.nav.k9.sak.behandlingslager.behandling.repository;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.util.Tuple;

@Dependent
public class BehandlingRevurderingRepository {

    private static final String AVSLUTTET_KEY = "avsluttet";

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private SøknadRepository søknadRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    BehandlingRevurderingRepository() {
    }

    @Inject
    public BehandlingRevurderingRepository(EntityManager entityManager,
                                               BehandlingRepository behandlingRepository,
                                               SøknadRepository søknadRepository,
                                               BehandlingLåsRepository behandlingLåsRepository) {

        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository);
        this.søknadRepository = Objects.requireNonNull(søknadRepository);
        this.behandlingLåsRepository = Objects.requireNonNull(behandlingLåsRepository);
    }

    EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Hent første henlagte endringssøknad etter siste innvilgede behandlinger for en fagsak
     */
    public List<Behandling> finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(Long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId"); // NOSONAR //$NON-NLS-1$

        Optional<Behandling> sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);

        if (sisteInnvilgede.isPresent()) {
            final List<Long> behandlingsIder = finnHenlagteBehandlingerEtter(fagsakId, sisteInnvilgede.get());
            for (Long behandlingId : behandlingsIder) {
                behandlingLåsRepository.taLås(behandlingId);
            }
            return behandlingsIder.stream()
                .map(behandlingId -> behandlingRepository.hentBehandling(behandlingId))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Optional<Behandling> hentSisteYtelsesbehandling(Long fagsakId) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
    }

    private List<Long> finnHenlagteBehandlingerEtter(Long fagsakId, Behandling sisteInnvilgede) {
        TypedQuery<Long> query = getEntityManager().createQuery(
            "SELECT b.id FROM Behandling b WHERE b.fagsak.id=:fagsakId " +
                " AND b.behandlingType=:type" +
                " AND b.opprettetTidspunkt >= :etterTidspunkt" +
                " AND b.behandlingResultatType IN :henlagtKoder " +
                " ORDER BY b.opprettetTidspunkt ASC", //$NON-NLS-1$
            Long.class);
        query.setParameter("fagsakId", fagsakId); //$NON-NLS-1$
        query.setParameter("type", BehandlingType.REVURDERING);
        query.setParameter("henlagtKoder", BehandlingResultatType.getAlleHenleggelseskoder());
        query.setParameter("etterTidspunkt", sisteInnvilgede.getOpprettetDato());
        return query.getResultList();
    }

    public Optional<LocalDate> finnSøknadsdatoFraHenlagtBehandling(Behandling behandling) {
        List<Behandling> henlagteBehandlinger = finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        Optional<SøknadEntitet> søknad = finnFørsteSøknadBlantBehandlinger(henlagteBehandlinger);
        if (søknad.isPresent()) {
            return Optional.ofNullable(søknad.get().getSøknadsdato());
        }
        return Optional.empty();
    }

    private Optional<SøknadEntitet> finnFørsteSøknadBlantBehandlinger(List<Behandling> behandlinger) {
        return behandlinger.stream()
            .map(behandling -> søknadRepository.hentSøknadHvisEksisterer(behandling.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    /** Liste av fagsakId, aktørId for saker som trenger G-regulering og det ikke finnes åpen behandling */
    public List<Tuple<Long, AktørId>> finnSakerMedBehovForGrunnbeløpRegulering(long gjeldende, long forrige, long forrigeAvkortingMultiplikator,
                                                                               LocalDate gjeldendeFom) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som har en avsluttet behandling med gammel sats og er avkortet til 6G og har uttak etter gammel sats sin utløpsdato
         * - Saken har ikke noen avsluttet behandling med ny sats
         * - Saken har ikke noen åpne ytelsesbehandlinger
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        List<String> ytelseBehandling = BehandlingType.getYtelseBehandlingTyper().stream().map(BehandlingType::getKode).collect(Collectors.toList());
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id , br.aktoer_id " +
                "from Fagsak f join bruker br on f.bruker_id=br.id " +
                "where f.id in ( " +
                "  select beh.fagsak_id from behandling beh " +
                "    join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=beh.id and grbg.aktiv = TRUE) " +
                "    join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id " +
                "    join BEREGNINGSGRUNNLAG_PERIODE bgper on grbg.beregningsgrunnlag_id=bgper.beregningsgrunnlag_id  " +
                "    join BR_RESULTAT_BEHANDLING grbr on (grbr.behandling_id=beh.id and grbr.aktiv = TRUE) " +
                "  where beh.behandling_status in (:avsluttet) " +
                "    and beh.behandling_resultat_type in (:restyper) " +
                "    and grbr.BG_BEREGNINGSRESULTAT_FP_ID in ( " +
                "      select beregningsresultat_fp_id from BR_PERIODE " +
                "      group by beregningsresultat_fp_id " +
                "      having min(br_periode_fom) >= :fomdato ) " +
                "    and bgper.avkortet_pr_aar is not null and bgper.avkortet_pr_aar = (bglag.grunnbeloep * :avkorting )  " +
                "    and bglag.grunnbeloep=:gmlsats ) " +
                " and f.id not in ( " +
                "    select beh.fagsak_id from behandling beh " +
                "    join GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=beh.id and grbg.aktiv=TRUE) " +
                "    join BEREGNINGSGRUNNLAG bglag on grbg.beregningsgrunnlag_id=bglag.id " +
                "    where beh.behandling_status in ( :avsluttet ) " +
                "      and bglag.grunnbeloep= :nysats ) " +
                " and f.id not in (" +
                "    select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in ( :avsluttet) " +
                "      and beh.behandling_type in (:ytelse) ) "); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("nysats", gjeldende); //$NON-NLS-1$ 7
        query.setParameter("gmlsats", forrige); //$NON-NLS-1$
        query.setParameter("avkorting", forrigeAvkortingMultiplikator); //$NON-NLS-1$
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.INNVILGET_ENDRING.getKode())); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, avsluttendeStatus); // $NON-NLS-1$
        query.setParameter("ytelse", ytelseBehandling); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new Tuple<>(((BigInteger) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }

    /** Liste av fagsakId, aktørId for saker som trenger Arena-regulering og det ikke finnes åpen behandling */
    public List<Tuple<Long, AktørId>> finnSakerMedBehovForArenaRegulering(LocalDate gjeldendeFom, LocalDate nySatsDato) {
        /*
         * Plukker fagsakId, aktørId fra fagsaker som møter disse kriteriene:
         * - Saker som er beregnet med AAP/DP og der FP-startdato overlapper inputIntervall
         * - Saken har ikke noen beregninger opprettet på eller etter dato for ny sats
         * - Saken har ikke noen åpne ytelsesbehandlinger
         */
        List<String> avsluttendeStatus = BehandlingStatus.getFerdigbehandletStatuser().stream().map(BehandlingStatus::getKode).collect(Collectors.toList());
        List<String> ytelseBehandling = BehandlingType.getYtelseBehandlingTyper().stream().map(BehandlingType::getKode).collect(Collectors.toList());
        Query query = getEntityManager().createNativeQuery(
            "SELECT DISTINCT f.id, bru.aktoer_id " +
                "FROM Fagsak f join bruker bru on f.bruker_id=bru.id " +
                "  JOIN behandling b on (fagsak_id = f.id and behandling_status in (:avsluttet) ) " +
                "  JOIN GR_BEREGNINGSGRUNNLAG grbg on (grbg.behandling_id=b.id and grbg.aktiv=TRUE) " +
                "  JOIN BG_AKTIVITET_STATUS bgs ON (bgs.BEREGNINGSGRUNNLAG_ID = grbg.BEREGNINGSGRUNNLAG_ID and bgs.AKTIVITET_STATUS in (:asarena) ) " +
                "  JOIN ( select grbr.behandling_id, min(brp.br_periode_fom) as fom " +
                "         from BR_RESULTAT_BEHANDLING grbr " +
                "         join BR_PERIODE brp on grbr.BG_BEREGNINGSRESULTAT_FP_ID = brp.beregningsresultat_fp_id " +
                "         where grbr.aktiv=TRUE " +
                "         group by grbr.behandling_id " +
                "         ) ford ON ford.behandling_id = b.id and ford.fom >= :fomdato and ford.fom <= :satsdato " +
                "WHERE coalesce(b.sist_oppdatert_tidspunkt, b.opprettet_tid) < :satsdato " +
                "  and b.behandling_resultat_type in (:restyper) " +
                "  and b.id not in (select ib.id from behandling ib " +
                "     join GR_BEREGNINGSGRUNNLAG igrbg on (igrbg.behandling_id=ib.id and igrbg.aktiv=TRUE) " +
                "     JOIN BG_AKTIVITET_STATUS ibgs ON (ibgs.BEREGNINGSGRUNNLAG_ID = igrbg.BEREGNINGSGRUNNLAG_ID and ibgs.AKTIVITET_STATUS in (:asarena) ) " +
                "    where coalesce(ib.sist_oppdatert_tidspunkt, ib.opprettet_tid) >= :satsdato " +
                "     and ib.behandling_resultat_type in (:restyper) " +
                "     and ib.behandling_status in (:avsluttet) ) " +
                "  and f.id not in (select beh.fagsak_id from behandling beh " +
                "    where beh.behandling_status not in (:avsluttet) " +
                "      and beh.behandling_type in (:ytelse) ) "); //$NON-NLS-1$
        query.setParameter("fomdato", gjeldendeFom); //$NON-NLS-1$
        query.setParameter("satsdato", nySatsDato); //$NON-NLS-1$
        query.setParameter("asarena", List.of(AktivitetStatus.ARBEIDSAVKLARINGSPENGER.getKode(), AktivitetStatus.DAGPENGER.getKode())); //$NON-NLS-1$
        query.setParameter("restyper", List.of(BehandlingResultatType.INNVILGET.getKode(), BehandlingResultatType.INNVILGET_ENDRING.getKode())); //$NON-NLS-1$
        query.setParameter(AVSLUTTET_KEY, avsluttendeStatus); // $NON-NLS-1$
        query.setParameter("ytelse", ytelseBehandling); //$NON-NLS-1$
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        return resultatList.stream().map(row -> new Tuple<>(((BigInteger) row[0]).longValue(), new AktørId((String) row[1]))).collect(Collectors.toList()); // NOSONAR
    }
}
