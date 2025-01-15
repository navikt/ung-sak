package no.nav.ung.sak.domene.registerinnhenting.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoGrunnlag;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.domene.registerinnhenting.SøknadDokumentTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef
public class UngdomsytelseSøknadDokumentTjeneste implements SøknadDokumentTjeneste {

    private UngdomsytelseStartdatoRepository repository;

    UngdomsytelseSøknadDokumentTjeneste() {
    }

    @Inject
    public UngdomsytelseSøknadDokumentTjeneste(UngdomsytelseStartdatoRepository repository) {
        this.repository = repository;
    }

    @Override
    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        return repository.hentGrunnlag(behandlingId)
            .map(UngdomsytelseStartdatoGrunnlag::getId)
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
        return UngdomsytelseStartdatoGrunnlag.class;
    }
}
