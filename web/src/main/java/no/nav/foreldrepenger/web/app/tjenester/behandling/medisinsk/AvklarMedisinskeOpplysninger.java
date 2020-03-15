package no.nav.foreldrepenger.web.app.tjenester.behandling.medisinsk;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.InnleggelsePeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsyn;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsynBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.KontinuerligTilsynPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæringer;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.AvklarMedisinskeOpplysningerDto;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.Legeerklæring;
import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.Pleiebehov;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarMedisinskeOpplysningerDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarMedisinskeOpplysninger implements AksjonspunktOppdaterer<AvklarMedisinskeOpplysningerDto> {

    private MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste;

    AvklarMedisinskeOpplysninger() {
        // for CDI proxy
    }

    @Inject
    AvklarMedisinskeOpplysninger(MedisinskGrunnlagRepository medisinskGrunnlagRepository, VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.tilVurderingTjeneste = tilVurderingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarMedisinskeOpplysningerDto dto, AksjonspunktOppdaterParameter param) {
        final var medisinskGrunnlag = medisinskGrunnlagRepository.hentHvisEksisterer(param.getBehandlingId());
        final var periode = utledPerioder(param.getBehandlingId());

        final var legeerklæringer = mapLegeerklæringer(medisinskGrunnlag.map(MedisinskGrunnlag::getLegeerklæringer).orElse(null), dto.getLegeerklæring());
        final var kontinuerligTilsyn = mapKontinuerligTilsyn(periode, medisinskGrunnlag.map(MedisinskGrunnlag::getKontinuerligTilsyn).orElse(null), dto.getPleiebehov());

        medisinskGrunnlagRepository.lagre(param.getBehandlingId(), kontinuerligTilsyn, legeerklæringer);

        return OppdateringResultat.utenOveropp();
    }

    private DatoIntervallEntitet utledPerioder(Long behandlingId) {
        final var perioder = tilVurderingTjeneste.utled(behandlingId, VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
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

    private KontinuerligTilsynBuilder mapKontinuerligTilsyn(DatoIntervallEntitet periode, KontinuerligTilsyn kontinuerligTilsyn, Pleiebehov pleiebehov) {
        final var builder = kontinuerligTilsyn != null ? KontinuerligTilsynBuilder.builder(kontinuerligTilsyn) : KontinuerligTilsynBuilder.builder();
        builder.tilbakeStill(periode);

        if (pleiebehov.getPerioderMedTilsynOgPleie() != null) {
            pleiebehov.getPerioderMedTilsynOgPleie()
                .stream()
                .map(it -> new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFom(), it.getPeriode().getTom()),
                    it.getBegrunnelse(), 100, it.getÅrsaksammenhengBegrunnelse(), it.getÅrsaksammenheng()))
                .forEach(builder::leggTil);
        }

        if (pleiebehov.getPerioderMedUtvidetTilsynOgPleie() != null) {
            pleiebehov.getPerioderMedUtvidetTilsynOgPleie()
                .stream()
                .map(it -> new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(it.getPeriode().getFom(), it.getPeriode().getTom()), it.getBegrunnelse(), 200))
                .forEach(builder::leggTil);
        }

        return builder;
    }

    private Legeerklæringer mapLegeerklæringer(Legeerklæringer legeerklæringer, List<Legeerklæring> dtoLegeerklæringer) {
        final var oppdatertLegeerklæringer = new Legeerklæringer(legeerklæringer);

        dtoLegeerklæringer.stream()
            .map(it -> {
                final var innleggelsePerioder = it.getInnleggelsesperioder()
                    .stream()
                    .map(at -> new InnleggelsePeriode(DatoIntervallEntitet.fraOgMedTilOgMed(at.getFom(), at.getTom())))
                    .collect(Collectors.toSet());
                return new no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Legeerklæring(it.getIdentifikator(), it.getFom(), innleggelsePerioder, it.getKilde(), it.getDiagnosekode());
            })
            .forEach(oppdatertLegeerklæringer::leggTilLegeerklæring);

        return oppdatertLegeerklæringer;
    }
}
