package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.jpa.QueryHints;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraph;
import no.nav.ung.sak.typer.AktørId;

/**
 * Dette er et Repository for håndtering av alle persistente endringer i en Personopplysning graf.
 * Personopplysning graf har en rot, representert ved Søkers Personopplysning innslag. Andre innslag kan være Barn eller
 * Partner.
 * <p>
 * Hent opp og lagre innhentende Personopplysning data, fra søknad, register (TPS) eller som avklart av Saksbehandler.
 * Ved hver endring kopieres Personopplysning grafen (inklusiv Familierelasjon) som et felles
 * Aggregat (ref. Domain Driven Design - Aggregat pattern)
 * <p>
 * Søkers Personopplysning representerer rot i grafen. Denne linkes til Behandling gjennom et
 * PersonopplysningGrunnlagEntitet.
 * <p>
 * Merk: standard regler - et Grunnlag eies av en Behandling. Et Aggregat (Søkers Personopplysning graf) har en
 * selvstenig livssyklus og vil kopieres ved hver endring.
 * Ved multiple endringer i et grunnlat for en Behandling vil alltid kun et innslag i grunnlag være aktiv for angitt
 * Behandling.
 */
@Dependent
public class PersonopplysningRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    protected PersonopplysningRepository() {
        // CDI
    }

    @Inject
    public PersonopplysningRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingRepository = new BehandlingRepository(entityManager);
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        Optional<PersonopplysningGrunnlagEntitet> eksisterendeGrunnlag = getAktivtGrunnlag(eksisterendeBehandlingId);

        final PersonopplysningGrunnlagBuilder builder = PersonopplysningGrunnlagBuilder.oppdatere(eksisterendeGrunnlag);

        lagreOgFlush(nyBehandlingId, builder);
    }

    public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long eksisterendeBehandlingId, Long nyBehandlingId) {
        Optional<PersonopplysningGrunnlagEntitet> eksisterendeGrunnlag = getAktivtGrunnlag(eksisterendeBehandlingId);

        final PersonopplysningGrunnlagBuilder builder = PersonopplysningGrunnlagBuilder.oppdatere(eksisterendeGrunnlag);
        builder.medOverstyrtVersjon(null);

        lagreOgFlush(nyBehandlingId, builder);
    }

    private DiffEntity personopplysningDiffer() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
        return getAktivtGrunnlag(behandlingId).orElse(null);
    }

    public DiffResult diffResultat(PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2, boolean onlyCheckTrackedFields) {
        return new RegisterdataDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Optional<PersonopplysningGrunnlagEntitet> pbg = getAktivtGrunnlag(behandlingId);
        PersonopplysningGrunnlagEntitet entitet = pbg.orElse(null);
        return Optional.ofNullable(entitet);
    }

    public void beskyttAktørId(AktørId aktørId) {
        final List<Long> behandlingIder = finnBehandlingerMedAdresseInformasjonFor(aktørId);

        for (Long behandlingId : behandlingIder) {
            entityManager.createNativeQuery("UPDATE vr_vilkar_periode vp\n"
                    + "SET regel_input = NULL\n"
                    + "WHERE EXISTS (\n"
                    + "  SELECT *\n"
                    + "  FROM vr_vilkar vv INNER JOIN rs_vilkars_resultat rs ON (\n"
                    + "        rs.vilkarene_id = vv.vilkar_resultat_id\n"
                    + "    ) \n"
                    + "  WHERE vv.id = vp.vilkar_id\n"
                    + "    AND rs.behandling_id = :behandlingId\n"
                    + ")")
                .setParameter("behandlingId", behandlingId)
                .executeUpdate();
        }

        entityManager.createNativeQuery("DELETE FROM PO_ADRESSE WHERE AKTOER_ID = :aktor")
            .setParameter("aktor", aktørId.getId())
            .executeUpdate();

        entityManager.flush();
    }

    private List<Long> finnBehandlingerMedAdresseInformasjonFor(AktørId aktørId) {
        @SuppressWarnings("unchecked") final List<Long> result = entityManager.createNativeQuery("SELECT DISTINCT g.behandling_id\n"
                + "FROM GR_PERSONOPPLYSNING g INNER JOIN PO_INFORMASJON i ON (\n"
                + "    g.registrert_informasjon_id = i.id\n"
                + "    OR g.overstyrt_informasjon_id = i.id\n"
                + "  ) INNER JOIN PO_ADRESSE a ON (\n"
                + "    i.id = a.po_informasjon_id\n"
                + "  )\n"
                + "WHERE a.aktoer_id = :aktor")
            .setParameter("aktor", aktørId.getId())
            .getResultList();

        return result;
    }

    private Optional<PersonopplysningGrunnlagEntitet> getAktivtGrunnlag(Long behandlingId) {
        TypedQuery<PersonopplysningGrunnlagEntitet> query = entityManager.createQuery(
                "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.behandlingId = :behandling_id AND pbg.aktiv = true", // NOSONAR //$NON-NLS-1$
                PersonopplysningGrunnlagEntitet.class)
            .setHint(QueryHints.HINT_CACHE_MODE, "IGNORE")
            .setParameter("behandling_id", behandlingId); // NOSONAR //$NON-NLS-1$

        Optional<PersonopplysningGrunnlagEntitet> resultat = HibernateVerktøy.hentUniktResultat(query);

        return resultat;
    }

    private void lagreOgFlush(Long behandlingId, PersonopplysningGrunnlagBuilder grunnlagBuilder) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder"); // NOSONAR //$NON-NLS-1$

        final Optional<PersonopplysningGrunnlagEntitet> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);

        final DiffEntity diffEntity = personopplysningDiffer();

        final PersonopplysningGrunnlagEntitet build = grunnlagBuilder.build();
        build.setBehandlingId(behandlingId);

        if (diffEntity.areDifferent(aktivtGrunnlag.orElse(null), build)) {
            aktivtGrunnlag.ifPresent(grunnlag -> {
                // setter gammelt grunnlag inaktiv. Viktig å gjøre før nye endringer siden vi kun
                // tillater ett aktivt grunnlag per behandling
                grunnlag.setAktiv(false);
                entityManager.persist(grunnlag);
                entityManager.flush();
            });

            build.getRegisterVersjon().ifPresent(this::persisterPersonInformasjon);
            build.getOverstyrtVersjon().ifPresent(this::persisterPersonInformasjon);

            entityManager.persist(build);
            entityManager.flush();
        }
    }

    private void persisterPersonInformasjon(PersonInformasjonEntitet registerVersjon) {
        entityManager.persist(registerVersjon);
        for (PersonRelasjonEntitet entitet : registerVersjon.getRelasjoner()) {
            entityManager.persist(entitet);
        }
        for (PersonopplysningEntitet entitet : registerVersjon.getPersonopplysninger()) {
            entityManager.persist(entitet);
        }
    }

    public void lagre(Long behandlingId, PersonInformasjonBuilder personInformasjonBuilder) {
        validerHarFasteAktører(behandlingId, personInformasjonBuilder);

        final PersonopplysningGrunnlagBuilder nyttGrunnlag = getGrunnlagBuilderFor(behandlingId);

        if (personInformasjonBuilder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            nyttGrunnlag.medRegistrertVersjon(personInformasjonBuilder);
        }
        if (personInformasjonBuilder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
            nyttGrunnlag.medOverstyrtVersjon(personInformasjonBuilder);
        }

        lagreOgFlush(behandlingId, nyttGrunnlag);
    }

    private void validerHarFasteAktører(Long behandlingId, PersonInformasjonBuilder personInformasjonBuilder) {
        Objects.requireNonNull(behandlingId);
        Objects.requireNonNull(personInformasjonBuilder);
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (!personInformasjonBuilder.harAktørId(behandling.getAktørId())) {
            throw new IllegalStateException("Mangler personinfo for brukers aktørId");
        }
    }

    private PersonopplysningGrunnlagBuilder getGrunnlagBuilderFor(Long behandlingId) {
        final Optional<PersonopplysningGrunnlagEntitet> aktivtGrunnlag = getAktivtGrunnlag(behandlingId);
        return PersonopplysningGrunnlagBuilder.oppdatere(aktivtGrunnlag);
    }

    /**
     * Tar med tidligere overstyring når lager ny overstyrt versjon.
     */
    public PersonInformasjonBuilder opprettBuilderFraEksisterende(Long behandlingId, PersonopplysningVersjonType type) {
        var aktivtGrunnlag = getAktivtGrunnlag(behandlingId);
        if (aktivtGrunnlag.isEmpty()) {
            // tillater ikke overstyring hvis ikke har grunnlag fra før
            if (type == PersonopplysningVersjonType.OVERSTYRT) {
                throw new IllegalStateException("Forventer å ha grunnlag før forsøker å overstyre");
            }
            return new PersonInformasjonBuilder(type);
        }
        var eksisterende = type == PersonopplysningVersjonType.REGISTRERT
            ? aktivtGrunnlag.get().getRegisterVersjon()
            : aktivtGrunnlag.get().getOverstyrtVersjon();
        return eksisterende.isEmpty() ? new PersonInformasjonBuilder(type) : new PersonInformasjonBuilder(eksisterende.get(), type);
    }

    private Optional<PersonopplysningGrunnlagEntitet> getInitiellVersjonAvPersonopplysningBehandlingsgrunnlag(
        Long behandlingId) {
        // må også sortere på id da opprettetTidspunkt kun er til nærmeste millisekund og ikke satt fra db.
        TypedQuery<PersonopplysningGrunnlagEntitet> query = entityManager.createQuery(
                "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.behandlingId = :behandling_id order by pbg.opprettetTidspunkt, pbg.id", //$NON-NLS-1$
                PersonopplysningGrunnlagEntitet.class)
            .setParameter("behandling_id", behandlingId) // NOSONAR
            .setMaxResults(1);

        Optional<PersonopplysningGrunnlagEntitet> resultat = query.getResultStream().findFirst();

        return resultat;
    }

    public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
        Optional<PersonopplysningGrunnlagEntitet> optGrunnlag = getInitiellVersjonAvPersonopplysningBehandlingsgrunnlag(behandlingId);
        return optGrunnlag.orElse(null);
    }

    public PersonopplysningGrunnlagEntitet hentPersonopplysningerPåId(Long aggregatId) {
        Optional<PersonopplysningGrunnlagEntitet> optGrunnlag = getVersjonAvPersonopplysningBehandlingsgrunnlagPåId(
            aggregatId);

        return optGrunnlag.orElse(null);
    }

    private Optional<PersonopplysningGrunnlagEntitet> getVersjonAvPersonopplysningBehandlingsgrunnlagPåId(
        Long aggregatId) {
        TypedQuery<PersonopplysningGrunnlagEntitet> query = entityManager.createQuery(
                "SELECT pbg FROM PersonopplysningGrunnlagEntitet pbg WHERE pbg.id = :aggregatId", //$NON-NLS-1$
                PersonopplysningGrunnlagEntitet.class)
            .setParameter("aggregatId", aggregatId); // NOSONAR //$NON-NLS-1$

        Optional<PersonopplysningGrunnlagEntitet> resultat = query.getResultStream().findFirst();

        return resultat;
    }
}
