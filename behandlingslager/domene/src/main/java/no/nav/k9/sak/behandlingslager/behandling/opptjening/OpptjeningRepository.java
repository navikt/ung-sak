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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class OpptjeningRepository {

    private EntityManager em;
    private BehandlingRepository behandlingRepository;

    public OpptjeningRepository() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningRepository(EntityManager em, BehandlingRepository behandlingRepository) {
        Objects.requireNonNull(em, "em"); //$NON-NLS-1$
        Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.em = em;
        this.behandlingRepository = behandlingRepository;
    }

    /**
     * Finn gjeldende opptjening for denne behandlingen.
     */
    public Optional<Opptjening> finnOpptjening(Long behandlingId) {
        return hentTidligereOpptjening(behandlingId, true);
    }

    public Optional<Opptjening> finnOpptjening(Vilkårene vilkårene) {

        return hentTidligereOpptjening(vilkårene.getId(), true);
    }

    private Optional<Opptjening> hentTidligereOpptjening(Long behandlingId, boolean readOnly) {
        // slår opp med HQL istedf. å traverse grafen
        TypedQuery<Opptjening> query = em.createQuery("from Opptjening o where o.behandling.id=:id and o.aktiv = TRUE", Opptjening.class); //$NON-NLS-1$
        query.setParameter("id", behandlingId); //$NON-NLS-1$

        if (readOnly) {
            // returneres read-only, kan kun legge til nye ved skriving uten å oppdatere
            query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        }
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Optional<Opptjening> deaktivereTidligereOpptjening(Long vilkårResultatId, boolean readOnly) {
        Optional<Opptjening> opptjening = hentTidligereOpptjening(vilkårResultatId, readOnly);
        if (opptjening.isPresent()) {
            Query query = em.createNativeQuery("UPDATE OPPTJENING SET AKTIV = FALSE WHERE ID=:id"); //$NON-NLS-1$
            query.setParameter("id", opptjening.get().getId()); //$NON-NLS-1$
            query.executeUpdate();
            em.flush();
            return opptjening;
        }
        return opptjening;
    }

    /**
     * Lagre Opptjeningsperiode (fom, tom) for en gitt behandling.
     */
    public Opptjening lagreOpptjeningsperiode(Behandling behandling, LocalDate opptjeningFom, LocalDate opptjeningTom, boolean skalBevareResultat) {

        Function<Opptjening, Opptjening> oppdateringsfunksjon = (tidligereOpptjening) -> {
            // lager ny opptjening alltid ved ny opptjeningsperiode.
            Opptjening nyOpptjening = new Opptjening(opptjeningFom, opptjeningTom);
            if (skalBevareResultat) {
                Set<OpptjeningAktivitet> kopiListe = duplikatSjekk(tidligereOpptjening.getOpptjeningAktivitet());
                nyOpptjening.setOpptjentPeriode(tidligereOpptjening.getOpptjentPeriode());
                nyOpptjening.setOpptjeningAktivitet(kopiListe);
            }
            return nyOpptjening;
        };

        Opptjening opptjening = lagre(behandling, oppdateringsfunksjon);

        return opptjening;
    }

    public void deaktiverOpptjening(Behandling behandling) {
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        deaktivereTidligereOpptjening(behandling.getId(), false);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);
    }

    private Opptjening lagre(Behandling behandling, Function<Opptjening, Opptjening> oppdateringsfunksjon) {
        Long behandlingId = behandling.getId();

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);


        Opptjening tidligereOpptjening = null;
        Opptjening opptjening;
        Optional<Opptjening> optTidligereOpptjening = deaktivereTidligereOpptjening(behandlingId, false);
        if (optTidligereOpptjening.isPresent()) {
            tidligereOpptjening = optTidligereOpptjening.get();
        }
        opptjening = oppdateringsfunksjon.apply(tidligereOpptjening);

        opptjening.setBehandling(behandling);

        em.persist(opptjening);
        em.flush();

        behandlingRepository.verifiserBehandlingLås(behandlingLås);

        return opptjening;

    }

    /**
     * Opptjening* Lagre opptjeningresultat (opptjent periode og aktiviteter).
     */
    public Opptjening lagreOpptjeningResultat(Behandling behandling, Period opptjentPeriode,
                                              Collection<OpptjeningAktivitet> opptjeningAktiviteter) {

        Set<OpptjeningAktivitet> kopiListe = duplikatSjekk(opptjeningAktiviteter);

        Function<Opptjening, Opptjening> oppdateringsfunksjon = (tidligereOpptjening) -> {
            Opptjening ny = new Opptjening(tidligereOpptjening);
            ny.setOpptjeningAktivitet(kopiListe);
            ny.setOpptjentPeriode(opptjentPeriode);
            return ny;
        };

        return lagre(behandling, oppdateringsfunksjon);
    }

    /**
     * Kopier over grunnlag til ny behandling
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Behandling origBehandling, Behandling nyBehandling) {
        // Opptjening er ikke koblet til Behandling gjennom aggregatreferanse. Må derfor kopieres som deep copy
        Long orgBehandlingId = origBehandling.getId();
        Opptjening origOpptjening = hentTidligereOpptjening(orgBehandlingId, true)
            .orElseThrow(() -> new IllegalStateException("Original behandling har ikke opptjening."));

        lagreOpptjeningsperiode(nyBehandling, origOpptjening.getFom(), origOpptjening.getTom(), false);
        lagreOpptjeningResultat(nyBehandling, origOpptjening.getOpptjentPeriode(), origOpptjening.getOpptjeningAktivitet());
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
        return finnOpptjening(behandlingId).map(Opptjening::getId);
    }

    //Denne metoden bør legges i Tjeneste
    public EndringsresultatSnapshot finnAktivGrunnlagId(Behandling behandling) {
        Optional<Long> funnetId = finnAktivOptjeningId(behandling.getId());
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(Opptjening.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(Opptjening.class));

    }

}
