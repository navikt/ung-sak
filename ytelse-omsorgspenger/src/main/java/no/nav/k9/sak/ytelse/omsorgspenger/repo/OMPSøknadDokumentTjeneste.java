package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.SøknadDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPSøknadDokumentTjeneste implements SøknadDokumentTjeneste {

    private OmsorgspengerGrunnlagRepository repository;

    OMPSøknadDokumentTjeneste() {
    }

    @Inject
    public OMPSøknadDokumentTjeneste(OmsorgspengerGrunnlagRepository repository) {
        this.repository = repository;
    }

    @Override
    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        return repository.hentGrunnlag(behandlingId)
            .map(OmsorgspengerGrunnlag::getId)
            .map(id -> EndringsresultatSnapshot.medSnapshot(getGrunnlagsKlasse(), id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(getGrunnlagsKlasse()));
    }

    @Override
    public DiffResult diffResultat(EndringsresultatDiff diff, boolean onlyTrackedFields) {
        var grunnlag1 = repository.hentGrunnlagBasertPåId((Long) diff.getGrunnlagId1()).orElse(null);
        var grunnlag2 = repository.hentGrunnlagBasertPåId((Long) diff.getGrunnlagId2()).orElse(null);

        return new RegisterdataDiffsjekker(onlyTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    @Override
    public Class<?> getGrunnlagsKlasse() {
        return OmsorgspengerGrunnlag.class;
    }
}
