package no.nav.foreldrepenger.web.app.tjenester.behandling.medisinsk;

import java.time.LocalDate;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarOmsorgenForDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOmsorgenForDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgenFor implements AksjonspunktOppdaterer<AvklarOmsorgenForDto> {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste;

    AvklarOmsorgenFor() {
        // for CDI proxy
    }

    @Inject
    AvklarOmsorgenFor(MedisinskGrunnlagRepository medisinskGrunnlagRepository, VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.tilVurderingTjeneste = tilVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(param.getBehandlingId());
        final var periode = utledPerioder(param.getBehandlingId());

        // TODO: implementer

        return OppdateringResultat.utenOveropp();
    }

    private DatoIntervallEntitet utledPerioder(Long behandlingId) {
        final var perioder = tilVurderingTjeneste.utled(behandlingId, VilkårType.OMSORGEN_FOR);
        final var fom = perioder.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo)
            .orElseThrow();
        final var tom = perioder.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo)
            .orElseThrow();

        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

}
