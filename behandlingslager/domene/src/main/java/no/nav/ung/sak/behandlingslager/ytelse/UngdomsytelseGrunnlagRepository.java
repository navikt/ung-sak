package no.nav.ung.sak.behandlingslager.ytelse;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPeriode;
import no.nav.ung.sak.diff.TraverseEntityGraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.diff.DiffEntity;
import no.nav.ung.sak.diff.DiffResult;
import no.nav.ung.sak.diff.TraverseGraph;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;

@Dependent
public class UngdomsytelseGrunnlagRepository {

    private static final Logger log = LoggerFactory.getLogger(UngdomsytelseGrunnlagRepository.class);
    private EntityManager entityManager;

    @Inject
    public UngdomsytelseGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(Long behandlingId, UngdomsytelseSatsResultat perioder) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new UngdomsytelseGrunnlag());

        var builder = new UngdomsytelseGrunnlagBuilder(aktivtGrunnlag);
        builder.medSatsPerioder(perioder.resultatTidslinje(), perioder.regelInput(), perioder.regelSporing());

        var differ = differ();

        if (builder.erForskjellig(aktivtGrunnlag, differ)) {
            grunnlagOptional.ifPresent(this::deaktiverEksisterende);
            lagre(builder, behandlingId);
        } else {
            log.info("[behandlingId={}] Forkaster lagring nytt resultat da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    /**
     * Deaktiverer grunnlag for en gitt behandlingId hvis det finnes et aktivt grunnlag.
     * Logger informasjon om deaktivering.
     *
     * @param behandlingId Id til behandlingen hvor grunnlaget skal deaktiveres
     */
    public void deaktiverGrunnlag(Long behandlingId) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        if (grunnlagOptional.isPresent()) {
            log.info("Deaktiverer eksisterende sats perioder for behandlingId={}", behandlingId);
            deaktiverEksisterende(grunnlagOptional.get());
        }
    }


    public void lagre(Long behandlingId, UngdomsytelseUttakPerioder uttakperioder) {
        var grunnlagOptional = hentGrunnlag(behandlingId);
        var aktivtGrunnlag = grunnlagOptional.orElse(new UngdomsytelseGrunnlag());

        var builder = new UngdomsytelseGrunnlagBuilder(aktivtGrunnlag);
        builder.medUttakPerioder(uttakperioder);

        var differ = differ();

        if (builder.erForskjellig(aktivtGrunnlag, differ)) {
            grunnlagOptional.ifPresent(this::deaktiverEksisterende);
            lagre(builder, behandlingId);
        } else {
            log.info("[behandlingId={}] Forkaster lagring nytt resultat da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    /**
     * Erstatter sats- og uttaksperiodene på {@code tilBehandlingId} med periodene på {@code fraBehandlingId}.
     * Har {@code fraBehandlingId} ikke grunnlag, deaktiveres grunnlaget på {@code tilBehandlingId}.
     * Brukes når en åpen behandling synkes med vedtatte data fra en annen behandling på fagsaken.
     */
    public void erstattGrunnlagMedKopiFra(Long fraBehandlingId, Long tilBehandlingId) {
        var fra = hentGrunnlag(fraBehandlingId);
        if (fra.isEmpty()) {
            deaktiverGrunnlag(tilBehandlingId);
            return;
        }
        var til = hentGrunnlag(tilBehandlingId);
        var builder = new UngdomsytelseGrunnlagBuilder(fra.get());
        if (builder.erForskjellig(til.orElse(null), differ())) {
            til.ifPresent(this::deaktiverEksisterende);
            lagre(builder, tilBehandlingId);
        }
    }

    private DiffEntity differ() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    private void deaktiverEksisterende(UngdomsytelseGrunnlag grunnlag) {
        grunnlag.setIkkeAktivt();
        entityManager.persist(grunnlag);
        entityManager.flush();
    }

    public Optional<UngdomsytelseGrunnlag> hentGrunnlag(Long behandlingId) {
        var query = entityManager.createQuery("SELECT bg FROM UngdomsytelseGrunnlag bg WHERE bg.behandlingId=:id AND bg.aktiv = true",UngdomsytelseGrunnlag.class)
            .setParameter("id", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        return hentGrunnlag(behandlingId)
            .map(grunnlag -> EndringsresultatSnapshot.medSnapshot(UngdomsytelseGrunnlag.class, grunnlag.getId()))
            .orElse(EndringsresultatSnapshot.utenSnapshot(UngdomsytelseGrunnlag.class));
    }

    /**
     * Diff på perioder med avslag i uttak, som avgrenser ytelsesperioden. Endret sats gir ingen diff.
     */
    public DiffResult diffAvslåttUttak(EndringsresultatDiff idEndring) {
        var avslag1 = avslagsårsakPerPeriode((Long) idEndring.getGrunnlagId1());
        var avslag2 = avslagsårsakPerPeriode((Long) idEndring.getGrunnlagId2());
        return differ().diff(avslag1, avslag2);
    }

    private Map<String, String> avslagsårsakPerPeriode(Long grunnlagId) {
        var grunnlag = Objects.requireNonNull(entityManager.find(UngdomsytelseGrunnlag.class, grunnlagId),
            "Fant ikke ytelsesgrunnlag med id " + grunnlagId);
        var avslag = new TreeMap<String, String>();
        if (grunnlag.getUttakPerioder() == null) {
            return avslag;
        }
        grunnlag.getAvslagstidslinjeFraUttak()
            .filterValue(uttak -> uttak.avslagsårsak() != null)
            .compress()
            .toSegments()
            .forEach(s -> avslag.put(s.getFom() + "|" + s.getTom(), s.getValue().avslagsårsak().getKode()));
        return avslag;
    }

    private void lagre(UngdomsytelseGrunnlagBuilder builder, Long behandlingId) {
        var oppdatertGrunnlag = builder.build();
        oppdatertGrunnlag.setBehandlingId(behandlingId);

        entityManager.persist(oppdatertGrunnlag.getSatsPerioder());
        if (oppdatertGrunnlag.getUttakPerioder() != null) {
            entityManager.persist(oppdatertGrunnlag.getUttakPerioder());
        }
        entityManager.persist(oppdatertGrunnlag);
        entityManager.flush();
    }

}
