package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.k9.sak.behandlingslager.diff.DiffResult;
import no.nav.k9.sak.domene.registerinnhenting.SøknadDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBSøknadDokumentTjeneste implements SøknadDokumentTjeneste {

    private UttakPerioderGrunnlagRepository repository;

    PSBSøknadDokumentTjeneste() {
    }

    @Inject
    public PSBSøknadDokumentTjeneste(UttakPerioderGrunnlagRepository repository) {
        this.repository = repository;
    }

    @Override
    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        return repository.hentGrunnlag(behandlingId)
            .map(UttaksPerioderGrunnlag::getId)
            .map(id -> EndringsresultatSnapshot.medSnapshot(getGrunnlagsKlasse(), id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(getGrunnlagsKlasse()));
    }

    @Override
    public DiffResult diffResultat(EndringsresultatDiff diff, boolean onlyTrackedFields) {
        var grunnlag1 = repository.hentGrunnlagBasertPåId((Long) diff.getGrunnlagId1());
        var grunnlag2 = repository.hentGrunnlagBasertPåId((Long) diff.getGrunnlagId2());

        return new RegisterdataDiffsjekker(onlyTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    @Override
    public Class<?> getGrunnlagsKlasse() {
        return UttaksPerioderGrunnlag.class;
    }
}
