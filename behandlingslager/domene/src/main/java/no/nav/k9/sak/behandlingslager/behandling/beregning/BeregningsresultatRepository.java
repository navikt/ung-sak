package no.nav.k9.sak.behandlingslager.behandling.beregning;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;

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
        return hentBeregningsresultatAggregat(behandlingId).map(BehandlingBeregningsresultatEntitet::getEndeligBeregningsresulat);
    }

    public Optional<BeregningsresultatEntitet> hentBgBeregningsresultat(Long behandlingId) {
        return hentBeregningsresultatAggregat(behandlingId).map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat);
    }

    public Optional<BeregningsresultatEntitet> hentUtbetBeregningsresultat(Long behandlingId) {
        return hentBeregningsresultatAggregat(behandlingId).map(BehandlingBeregningsresultatEntitet::getUtbetBeregningsresultat);
    }

    public Optional<BehandlingBeregningsresultatEntitet> hentBeregningsresultatAggregat(Long behandlingId) {
        TypedQuery<BehandlingBeregningsresultatEntitet> query = entityManager.createQuery(
            "from BeregningsresultatAggregatEntitet aggregat " +
                "where aggregat.behandlingId=:behandlingId and aggregat.aktiv = TRUE", BehandlingBeregningsresultatEntitet.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return hentUniktResultat(query);
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
        if (!aggregatOpt.isPresent()) {
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
