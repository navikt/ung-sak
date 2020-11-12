package no.nav.k9.sak.domene.abakus;

import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@Dependent
public class MottattInntektsmeldingRepository {

    private final EntityManager entityManager;

    @Inject
    public MottattInntektsmeldingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }


    public List<MottattInntektsmelding> hentAlle(Long id) {
        return null;
    }
}
