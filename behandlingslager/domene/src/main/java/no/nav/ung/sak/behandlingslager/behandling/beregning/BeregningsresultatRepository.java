package no.nav.ung.sak.behandlingslager.behandling.beregning;

import static no.nav.k9.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;

@Dependent
public class BeregningsresultatRepository {
    private static final long G_MULTIPLIKATOR = 6L;
    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;

    protected BeregningsresultatRepository() {
        // for CDI proxy
    }

    @Inject
    public BeregningsresultatRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
    }

    public Optional<BeregningsresultatEntitet> hentEndeligBeregningsresultat(Long behandlingId) {
        Optional<BehandlingBeregningsresultatEntitet> aggregat = hentBeregningsresultatAggregat(behandlingId);
        Optional<BeregningsresultatEntitet> utbet = aggregat
            .map(BehandlingBeregningsresultatEntitet::getUtbetBeregningsresultat);

        return utbet.isPresent() ? utbet : aggregat.map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat);
    }

    public Optional<BeregningsresultatEntitet> hentBgBeregningsresultat(Long behandlingId) {
        return hentBeregningsresultatAggregat(behandlingId).map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat);
    }

    public Optional<BehandlingBeregningsresultatEntitet> hentBeregningsresultatAggregat(Long behandlingId) {
        TypedQuery<BehandlingBeregningsresultatEntitet> query = entityManager.createQuery(
            "from BeregningsresultatAggregatEntitet aggregat " +
                "where aggregat.behandlingId=:behandlingId and aggregat.aktiv = TRUE", BehandlingBeregningsresultatEntitet.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return hentUniktResultat(query);
    }

    @SuppressWarnings("unchecked")
    public List<Behandling> hentSisteBehandlingerMedUtbetalingForDagpenger(FagsakYtelseType ytelseType, LocalDate fom, LocalDate tom) {
        String sql = """
            select DISTINCT ON (behandling.fagsak_id) behandling.* from BR_RESULTAT_BEHANDLING gr_br
            inner join BR_BEREGNINGSRESULTAT br on gr_br.bg_beregningsresultat_fp_id = br.id
            inner join BR_ANDEL andel on br.id = andel.BEREGNINGSRESULTAT_ID
            inner join BEHANDLING behandling on gr_br.behandling_id = behandling.id
            inner join FAGSAK fagsak on fagsak.id = behandling.fagsak_id
            where gr_br.aktiv = true
            and andel.inntektskategori = 'DAGPENGER'
            and andel.dagsats > 0
            and fagsak.ytelse_type = :ytelseType
            and andel.periode && daterange(cast(:fom as date), cast(:tom as date), '[]') = true
            order by behandling.fagsak_id, behandling.opprettet_tid desc
              """;

        var query = entityManager.createNativeQuery(sql, Behandling.class); //$NON-NLS-1$
        query.setParameter("ytelseType", ytelseType.getKode());
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);
        return query.getResultList();
    }


    public void lagre(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        BehandlingBeregningsresultatBuilder builder = opprettResultatBuilderFor(behandling.getId());
        builder.medBgBeregningsresultat(beregningsresultat);
        lagreOgFlush(behandling, builder);
    }

    public void lagreUtbetBeregningsresultat(Behandling behandling, BeregningsresultatEntitet utbetBeregningsresultatFP) {
        BehandlingBeregningsresultatBuilder builder = opprettResultatBuilderFor(behandling.getId());
        builder.medUtbetBeregningsresultat(utbetBeregningsresultatFP);
        lagreOgFlush(behandling, builder);
    }

    /**
     * Lagrer beregningsresultataggregatet med en verdi for hindreTilbaketrekk
     *
     * @param behandling en {@link Behandling}
     * @param skalHindreTilbaketrekk skal tilkjent ytelse omfordeles mellom bruker og arbeidsgiver?
     * @return Tidligere verdi
     */
    public Optional<Boolean> lagreMedTilbaketrekk(Behandling behandling, boolean skalHindreTilbaketrekk) {
        Long behandlingId = behandling.getId();
        Optional<BehandlingBeregningsresultatEntitet> aggregatOpt = hentBeregningsresultatAggregat(behandlingId);
        if (aggregatOpt.isEmpty()) {
            throw new IllegalStateException("Finner ikke beregningsresultataggregat for behandlingen" + behandlingId);
        }

        BehandlingBeregningsresultatBuilder builder = opprettResultatBuilderFor(aggregatOpt);
        builder.medSkalHindreTilbaketrekk(skalHindreTilbaketrekk);
        lagreOgFlush(behandling, builder);

        return aggregatOpt.flatMap(BehandlingBeregningsresultatEntitet::skalHindreTilbaketrekk);
    }

    private void lagreOgFlush(Behandling behandling, BehandlingBeregningsresultatBuilder builder) {
        Optional<BehandlingBeregningsresultatEntitet> tidligereAggregat = hentBeregningsresultatAggregat(behandling.getId());
        if (tidligereAggregat.isPresent()) {
            tidligereAggregat.get().deaktiver();
            entityManager.persist(tidligereAggregat.get());
            entityManager.flush();
        }
        BehandlingBeregningsresultatEntitet aggregatEntitet = builder.build(behandling.getId());
        entityManager.persist(aggregatEntitet.getBgBeregningsresultat());
        aggregatEntitet.getBgBeregningsresultat().getBeregningsresultatPerioder().forEach(this::lagre);
        if (aggregatEntitet.getUtbetBeregningsresultat() != null) {
            entityManager.persist(aggregatEntitet.getUtbetBeregningsresultat());
            aggregatEntitet.getUtbetBeregningsresultat().getBeregningsresultatPerioder().forEach(this::lagre);
        }
        entityManager.persist(aggregatEntitet);
        entityManager.flush();
    }

    private BehandlingBeregningsresultatBuilder opprettResultatBuilderFor(Long behandlingId) {
        Optional<BehandlingBeregningsresultatEntitet> aggregat = hentBeregningsresultatAggregat(behandlingId);
        return opprettResultatBuilderFor(aggregat);
    }

    private BehandlingBeregningsresultatBuilder opprettResultatBuilderFor(Optional<BehandlingBeregningsresultatEntitet> aggregat) {
        return BehandlingBeregningsresultatBuilder.oppdatere(aggregat);
    }

    private void lagre(BeregningsresultatPeriode beregningsresultatPeriode) {
        entityManager.persist(beregningsresultatPeriode);
        beregningsresultatPeriode.getBeregningsresultatAndelList().forEach(this::lagre);
    }

    private void lagre(BeregningsresultatAndel beregningsresultatAndel) {
        entityManager.persist(beregningsresultatAndel);
    }

    public void deaktiverBeregningsresultat(Long behandlingId, BehandlingLås skriveLås) {
        Optional<BehandlingBeregningsresultatEntitet> aggregatOpt = hentBeregningsresultatAggregat(behandlingId);
        aggregatOpt.ifPresent(aggregat -> {
            aggregat.deaktiver();
            entityManager.persist(aggregat);
            entityManager.flush();
        });
        verifiserBehandlingLås(skriveLås);
        entityManager.flush();
    }

    private void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }

    public long avkortingMultiplikatorG(@SuppressWarnings("unused") LocalDate dato) {
        return G_MULTIPLIKATOR;
    }
}
