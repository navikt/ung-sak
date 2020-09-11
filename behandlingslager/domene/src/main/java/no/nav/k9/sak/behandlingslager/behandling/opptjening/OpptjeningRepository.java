package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class OpptjeningRepository {

    private EntityManager em;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public OpptjeningRepository() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRepository(EntityManager em, BehandlingRepository behandlingRepository, VilkårResultatRepository vilkårResultatRepository) {
        Objects.requireNonNull(em, "em"); //$NON-NLS-1$
        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.em = em;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    /**
     * Finn gjeldende opptjening for denne behandlingen.
     */
    public Optional<OpptjeningResultat> finnOpptjening(Long behandlingId) {
        return hentTidligereOpptjening(behandlingId, true);
    }

    private Optional<OpptjeningResultat> hentTidligereOpptjening(Long behandlingId, boolean readOnly) {
        // slår opp med HQL istedf. å traverse grafen
        TypedQuery<OpptjeningResultat> query = em.createQuery("from OpptjeningResultat o where o.behandlingId=:id and o.aktiv = TRUE", OpptjeningResultat.class); //$NON-NLS-1$
        query.setParameter("id", behandlingId); //$NON-NLS-1$

        if (readOnly) {
            // returneres read-only, kan kun legge til nye ved skriving uten å oppdatere
            query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        }
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagre Opptjeningsperiode (fom, tom) for en gitt behandling.
     */
    public Opptjening lagreOpptjeningsperiode(Behandling behandling, LocalDate opptjeningFom, LocalDate opptjeningTom, boolean skalBevareResultat) {

        Function<OpptjeningResultatBuilder, Opptjening> oppdateringsfunksjon = (builder) -> {
            var tidligereOpptjening = builder.hentTidligereOpptjening(DatoIntervallEntitet.fraOgMedTilOgMed(opptjeningFom, opptjeningTom));
            // lager ny opptjening alltid ved ny opptjeningsperiode.
            Opptjening nyOpptjening = new Opptjening(opptjeningFom, opptjeningTom);
            if (tidligereOpptjening.isPresent() && skalBevareResultat) {
                Set<OpptjeningAktivitet> kopiListe = duplikatSjekk(tidligereOpptjening.get().getOpptjeningAktivitet());
                nyOpptjening.setOpptjentPeriode(tidligereOpptjening.get().getOpptjentPeriode());
                nyOpptjening.setOpptjeningAktivitet(kopiListe);
            }
            builder.leggTil(nyOpptjening);
            return nyOpptjening;
        };

        Opptjening opptjening = lagre(behandling, oppdateringsfunksjon, true);

        return opptjening;
    }

    private Opptjening lagre(Behandling behandling, Function<OpptjeningResultatBuilder, Opptjening> oppdateringsFunksjon, boolean ryddOpptjeningsresultat) {

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);

        var tidligereOpptjeninger = hentTidligereOpptjening(behandling.getId(), false);

        var builder = new OpptjeningResultatBuilder(tidligereOpptjeninger.orElse(null));
        var opptjening = oppdateringsFunksjon.apply(builder);

        if (ryddOpptjeningsresultat) {
            ryddMotVilkårsPerioder(behandling, builder);
        } else {
            validerMotVilkårsPerioder(behandling, builder);
        }
        tidligereOpptjeninger.ifPresent(this::deaktiverTidligereOpptjening);

        var opptjeningResultat = builder.build();
        opptjeningResultat.setBehandlingId(behandling.getId());
        em.persist(opptjeningResultat);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);
        return opptjening;
    }

    private void ryddMotVilkårsPerioder(Behandling behandling, OpptjeningResultatBuilder builder) {
        var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).flatMap(it -> it.getVilkår(VilkårType.OPPTJENINGSVILKÅRET));
        vilkår.ifPresent(builder::fjernOverflødigePerioder);
    }

    private void validerMotVilkårsPerioder(Behandling behandling, OpptjeningResultatBuilder builder) {
        var vilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId()).flatMap(it -> it.getVilkår(VilkårType.OPPTJENINGSVILKÅRET));
        vilkår.ifPresent(builder::validerMotVilkår);
    }

    private void deaktiverTidligereOpptjening(OpptjeningResultat tidligereOpptjeninger) {

        var query = em.createNativeQuery("UPDATE rs_opptjening set aktiv = false WHERE id = :id")
            .setParameter("id", tidligereOpptjeninger.getId());
        tidligereOpptjeninger.setInaktiv();
        query.executeUpdate();
        em.flush();
    }

    /**
     * Opptjening* Lagre opptjeningresultat (opptjent periode og aktiviteter).
     */
    public Opptjening lagreOpptjeningResultat(Behandling behandling, LocalDate skjæringstidspunkt,
                                              Period opptjentPeriode,
                                              Collection<OpptjeningAktivitet> opptjeningAktiviteter) {

        Set<OpptjeningAktivitet> kopiListe = duplikatSjekk(opptjeningAktiviteter);

        Function<OpptjeningResultatBuilder, Opptjening> oppdateringsfunksjon = (builder) -> {
            var tidligereOpptjening = builder.hentTidligereOpptjening(skjæringstidspunkt).orElseThrow();

            Opptjening ny = new Opptjening(tidligereOpptjening);
            ny.setOpptjeningAktivitet(kopiListe);
            ny.setOpptjentPeriode(opptjentPeriode);

            builder.leggTil(ny);
            return ny;
        };

        return lagre(behandling, oppdateringsfunksjon, false);
    }

    /**
     * Kopier over grunnlag til ny behandling
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long orgBehandlingId, Behandling nyBehandling) {
        // Opptjening er ikke koblet til Behandling gjennom aggregatreferanse. Må derfor kopieres som deep copy
        var origOpptjening = hentTidligereOpptjening(orgBehandlingId, true)
            .orElseThrow(() -> new IllegalStateException("Original behandling har ikke opptjening."));

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(nyBehandling);

        var builder = new OpptjeningResultatBuilder(origOpptjening);
        var opptjeningResultat = builder.build();

        opptjeningResultat.setBehandlingId(nyBehandling.getId());
        em.persist(opptjeningResultat);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);
    }

    private Set<OpptjeningAktivitet> duplikatSjekk(Collection<OpptjeningAktivitet> opptjeningAktiviteter) {
        // ta kopi av alle aktiviteter for å være sikker på at gamle ikke skrives inn.
        if (opptjeningAktiviteter == null) {
            return Collections.emptySet();
        }
        Set<OpptjeningAktivitet> kopiListe = opptjeningAktiviteter.stream()
            .map(OpptjeningAktivitet::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (opptjeningAktiviteter.size() > kopiListe.size()) {
            // har duplikater!!
            Set<OpptjeningAktivitet> duplikater = opptjeningAktiviteter.stream()
                .filter(oa -> Collections.frequency(opptjeningAktiviteter, oa) > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            throw new IllegalArgumentException(
                "Utvikler-feil: har duplikate opptjeningsaktiviteter: [" + duplikater + "] i input: " + opptjeningAktiviteter); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return kopiListe;
    }

    private Optional<Long> finnAktivOptjeningId(Long behandlingId) {
        return finnOpptjening(behandlingId).map(OpptjeningResultat::getId);
    }

    // Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivGrunnlagId(Behandling behandling) {
        Optional<Long> funnetId = finnAktivOptjeningId(behandling.getId());
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(OpptjeningResultat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(OpptjeningResultat.class));

    }

    public void deaktiverOpptjeningForPeriode(Behandling behandling, DatoIntervallEntitet vilkårsPeriode) {
        Function<OpptjeningResultatBuilder, Opptjening> oppdateringsfunksjon = (builder) -> {
            builder.deaktiver(vilkårsPeriode.getFomDato());
            return null;
        };

        lagre(behandling, oppdateringsfunksjon, false);
    }

    public void deaktiverOpptjening(Long behandlingId) {
        hentTidligereOpptjening(behandlingId, false).ifPresent(v -> deaktiverTidligereOpptjening(v));
    }
}
